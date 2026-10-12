
package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class IncidenciaDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: IncidenciaDao

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.incidenciaDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun la_bandeja_excluye_cerradas_y_ordena_de_la_mas_antigua() = runTest {
        val base = Instant.parse("2026-09-27T09:00:00Z")
        dao.guardarTodas(listOf(
            incidencia("a", MarcaSeguimiento.EN_ATENCION, base.plusSeconds(3600)).toEntity(false),
            incidencia("b", MarcaSeguimiento.ASIGNADA, base).toEntity(false),
            incidencia("c", MarcaSeguimiento.CERRADA, base.minusSeconds(3600)).toEntity(false)
        ))

        val bandeja = dao.observarBandeja("t1").first().map { it.toDomain() }

        assertEquals(listOf("b", "a"), bandeja.map { it.id })
    }

    @Test
    fun lo_registrado_sin_conexion_queda_pendiente_de_sincronizar() = runTest {
        dao.guardar(incidencia("x", MarcaSeguimiento.SIN_ASIGNAR, Instant.now()).toEntity(pendienteSincronizar = true))
        dao.guardar(incidencia("y", MarcaSeguimiento.SIN_ASIGNAR, Instant.now()).toEntity(pendienteSincronizar = false))

        assertEquals(listOf("x"), dao.pendientesDeSincronizar().map { it.id })
    }

    @Test
    fun un_rechazo_no_marca_una_version_editada_mientras_se_subia() = runTest {
        val subida = Instant.parse("2026-10-11T10:00:00Z")
        val editada = subida.plusSeconds(60)
        dao.guardar(incidencia("x", MarcaSeguimiento.ASIGNADA, subida).toEntity(true, actualizadoEn = editada))

        // RF-75: el rechazo llega para la versión que se subía, no para la editada
        assertEquals(0, dao.marcarNecesitaAtencion("x", subida))
        assertEquals(true, dao.porId("x")?.pendienteSincronizar)

        assertEquals(1, dao.marcarNecesitaAtencion("x", editada))
        val marcada = dao.porId("x")!!
        assertEquals(false, marcada.pendienteSincronizar)
        assertEquals(true, marcada.necesitaAtencion)
    }

    @Test
    fun los_rechazos_se_cuentan_por_incidencia_afectada() = runTest {
        val ahora = Instant.parse("2026-10-11T10:00:00Z")
        val movimientos = db.movimientoDao()
        dao.guardar(incidencia("a", MarcaSeguimiento.ASIGNADA, ahora).toEntity(true, actualizadoEn = ahora))
        movimientos.guardar(movimiento("a1", "a"))
        movimientos.guardar(movimiento("a2", "a"))
        movimientos.guardar(movimiento("b1", "b"))

        // La incidencia "a" y sus dos movimientos son un solo rechazo; el movimiento suelto de "b", otro
        dao.marcarNecesitaAtencion("a", ahora)
        movimientos.marcarNecesitaAtencionDe("a")
        movimientos.marcarNecesitaAtencion("b1")

        // RF-76 (D-8 de la spec 005)
        assertEquals(2, dao.observarAfectadasPorRechazo().first())
    }

    @Test
    fun solo_cuentan_las_asignaciones_cuyo_movimiento_aun_no_subio() = runTest {
        val ahora = Instant.parse("2026-10-11T10:00:00Z")
        val movimientos = db.movimientoDao()
        fun guardada(id: String, marca: MarcaSeguimiento, pendiente: Boolean, tecnico: String = "t1") =
            incidencia(id, marca, ahora).copy(tecnicoAsignadoId = tecnico).toEntity(pendiente, actualizadoEn = ahora)
        // Cuentan: asignada y reasignada en este celular, con su movimiento todavía pendiente
        dao.guardar(guardada("nueva", MarcaSeguimiento.ASIGNADA, pendiente = true))
        movimientos.guardar(movimiento("m-nueva", "nueva"))
        dao.guardar(guardada("reasignada", MarcaSeguimiento.ASIGNADA, pendiente = true))
        movimientos.guardar(movimiento("m-reasignada", "reasignada", TipoMovimiento.REASIGNADA))
        dao.guardar(guardada("de-t2", MarcaSeguimiento.ESPERANDO_REPUESTO, pendiente = true, tecnico = "t2"))
        movimientos.guardar(movimiento("m-de-t2", "de-t2", TipoMovimiento.REASIGNADA))
        // No cuentan: pendiente por otro motivo, ya subida, o cerrada
        dao.guardar(guardada("otro-motivo", MarcaSeguimiento.EN_ATENCION, pendiente = true))
        movimientos.guardar(movimiento("m-otro-motivo", "otro-motivo", TipoMovimiento.ATENCION_INICIADA))
        dao.guardar(guardada("subida", MarcaSeguimiento.ASIGNADA, pendiente = false))
        movimientos.guardar(movimiento("m-subida", "subida"))
        dao.guardar(guardada("cerrada", MarcaSeguimiento.CERRADA, pendiente = true))
        movimientos.guardar(movimiento("m-cerrada", "cerrada"))

        // RF-602, RF-612
        assertEquals(listOf("t1", "t1", "t2"), dao.observarAsignacionesSinSubir().first().sorted())
        assertEquals(2, dao.asignacionesSinSubir("t1"))
        assertEquals(1, dao.asignacionesSinSubir("t2"))
    }

    private fun movimiento(id: String, incidenciaId: String, tipo: TipoMovimiento = TipoMovimiento.ASIGNADA) =
        Movimiento(
            id = id, incidenciaId = incidenciaId, tipo = tipo,
            autorNombre = "Carlos Mendoza", fecha = Instant.parse("2026-10-11T10:00:00Z")
        ).toEntity(pendienteSincronizar = true)

    private fun incidencia(id: String, marca: MarcaSeguimiento, fecha: Instant) = Incidencia(
        id = id, numero = null, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = fecha, registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = marca,
        tecnicoAsignadoId = "t1", tecnicoAsignadoNombre = "Luis Ramírez"
    )
}