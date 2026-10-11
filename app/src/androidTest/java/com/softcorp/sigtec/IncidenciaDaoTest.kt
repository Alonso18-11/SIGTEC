
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

    private fun incidencia(id: String, marca: MarcaSeguimiento, fecha: Instant) = Incidencia(
        id = id, numero = null, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = fecha, registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = marca,
        tecnicoAsignadoId = "t1", tecnicoAsignadoNombre = "Luis Ramírez"
    )
}