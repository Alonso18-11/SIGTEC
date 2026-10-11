package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-05, segunda revisión: qué movimientos suben y cómo se marcan con su incidencia rechazada. */
@RunWith(AndroidJUnit4::class)
class MovimientoDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var incidencias: IncidenciaDao
    private lateinit var dao: MovimientoDao

    private val ahora = Instant.parse("2026-10-11T10:00:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        incidencias = db.incidenciaDao()
        dao = db.movimientoDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun solo_suben_los_movimientos_cuya_incidencia_ya_esta_en_el_servidor() = runTest {
        incidencias.guardar(incidencia("subida", pendiente = false))
        incidencias.guardar(incidencia("pendiente", pendiente = true))
        incidencias.guardar(incidencia("rechazada", pendiente = false, atencion = true))
        dao.guardar(movimiento("m-subida", "subida"))
        dao.guardar(movimiento("m-pendiente", "pendiente"))
        dao.guardar(movimiento("m-rechazada", "rechazada"))
        dao.guardar(movimiento("m-sin-incidencia", "no-esta-en-el-celular"))
        dao.guardar(movimiento("m-ya-subido", "subida", pendiente = false))

        // RF-68
        val listos = dao.listosParaSubir().map { it.id }.sorted()

        assertEquals(listOf("m-sin-incidencia", "m-subida"), listos)
    }

    @Test
    fun una_incidencia_rechazada_bloquea_solo_sus_movimientos_pendientes() = runTest {
        dao.guardar(movimiento("m1", "rechazada"))
        dao.guardar(movimiento("m2", "rechazada", pendiente = false))
        dao.guardar(movimiento("m3", "otra"))

        // RF-75, RF-79
        dao.marcarNecesitaAtencionDe("rechazada")

        val m1 = dao.porId("m1")!!
        assertEquals(false, m1.pendienteSincronizar)
        assertEquals(true, m1.necesitaAtencion)
        assertEquals(false, dao.porId("m2")!!.necesitaAtencion)   // ya estaba en el servidor
        assertEquals(true, dao.porId("m3")!!.pendienteSincronizar)
    }

    private fun incidencia(id: String, pendiente: Boolean, atencion: Boolean = false) = IncidenciaEntity(
        id = id, numero = 1, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = ahora, registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.ASIGNADA, tipoFalla = null,
        prioridad = null, tecnicoAsignadoId = "t1", tecnicoAsignadoNombre = "Luis Ramírez",
        critica = false, pendienteSincronizar = pendiente, actualizadoEn = ahora,
        necesitaAtencion = atencion
    )

    private fun movimiento(id: String, incidenciaId: String, pendiente: Boolean = true) = MovimientoEntity(
        id = id, incidenciaId = incidenciaId, tipo = TipoMovimiento.ASIGNADA,
        autorNombre = "Carlos Mendoza", fecha = ahora, detalle = null, pendienteSincronizar = pendiente
    )
}
