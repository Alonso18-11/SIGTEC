package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.SugerenciaIADao
import com.softcorp.sigtec.core.database.entity.SugerenciaIAEntity
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-14: la sugerencia sin decidir más reciente (RF-1405, RF-1424), la decisión (RF-1427) y lo que se sube (RF-1429). */
@RunWith(AndroidJUnit4::class)
class SugerenciaIADaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: SugerenciaIADao

    private val antes = Instant.parse("2026-11-16T10:00:00Z")
    private val despues = Instant.parse("2026-11-16T10:05:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.sugerenciaIADao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun sin_decidir_devuelve_la_mas_reciente_de_ese_tipo() = runTest {
        dao.guardar(sugerencia("b1", TipoSugerencia.BORRADOR_INFORME, antes))
        dao.guardar(sugerencia("b2", TipoSugerencia.BORRADOR_INFORME, despues))
        dao.guardar(sugerencia("c1", TipoSugerencia.CLASIFICACION, despues))

        assertEquals("b2", dao.observarSinDecidir("inc-1", TipoSugerencia.BORRADOR_INFORME).first()?.id)
        assertEquals("b2", dao.sinDecidir("inc-1", TipoSugerencia.BORRADOR_INFORME)?.id)
        assertEquals("c1", dao.sinDecidir("inc-1", TipoSugerencia.CLASIFICACION)?.id)
        assertNull(dao.sinDecidir("inc-2", TipoSugerencia.CLASIFICACION))
    }

    @Test
    fun contar_incluye_las_decididas() = runTest {
        dao.guardar(sugerencia("c1", TipoSugerencia.CLASIFICACION, antes))
        dao.decidir("c1", aceptada = false, decididaPorId = "u-jefe", decididaEn = despues)

        assertEquals(1, dao.contar("inc-1", TipoSugerencia.CLASIFICACION))
        assertEquals(0, dao.contar("inc-1", TipoSugerencia.BORRADOR_INFORME))
        assertNull(dao.sinDecidir("inc-1", TipoSugerencia.CLASIFICACION))
    }

    @Test
    fun decidir_guarda_quien_y_cuando_una_sola_vez() = runTest {
        dao.guardar(sugerencia("c1", TipoSugerencia.CLASIFICACION, antes))

        assertEquals(1, dao.decidir("c1", aceptada = true, decididaPorId = "u-jefe", decididaEn = despues))
        assertEquals(0, dao.decidir("c1", aceptada = false, decididaPorId = "u-otro", decididaEn = despues))

        val guardada = dao.porIncidencia("inc-1").single()
        assertEquals(true, guardada.aceptada)
        assertEquals("u-jefe", guardada.decididaPorId)
        assertEquals(despues, guardada.decididaEn)
        assertEquals(despues, guardada.actualizadoEn)
        assertTrue(guardada.pendienteSincronizar)
    }

    @Test
    fun solo_las_decididas_quedan_por_subir_hasta_marcarlas() = runTest {
        dao.guardar(sugerencia("c1", TipoSugerencia.CLASIFICACION, antes))
        dao.guardar(sugerencia("b1", TipoSugerencia.BORRADOR_INFORME, antes))
        dao.decidir("b1", aceptada = true, decididaPorId = "u-luis", decididaEn = despues)

        assertEquals(listOf("b1"), dao.pendientesDeSincronizar().map { it.id })
        assertEquals(1, dao.observarCantidadPendientes().first())

        dao.marcarSincronizada("b1")

        assertTrue(dao.pendientesDeSincronizar().isEmpty())
        assertEquals(0, dao.observarCantidadPendientes().first())
    }

    private fun sugerencia(id: String, tipo: TipoSugerencia, generadaEn: Instant) = SugerenciaIAEntity(
        id = id, incidenciaId = "inc-1", tipo = tipo, contenido = "{}", modelo = "gemini-3.5-flash-lite",
        generadaEn = generadaEn, aceptada = null, decididaPorId = null, decididaEn = null,
        pendienteSincronizar = false, actualizadoEn = generadaEn
    )
}
