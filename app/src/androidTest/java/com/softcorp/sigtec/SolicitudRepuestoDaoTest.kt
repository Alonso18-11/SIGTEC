package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.SolicitudRepuestoDao
import com.softcorp.sigtec.core.database.entity.SolicitudRepuestoEntity
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-11: qué solicitudes se registran en Logística, cuáles se consultan y cuántas faltan entregar. */
@RunWith(AndroidJUnit4::class)
class SolicitudRepuestoDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: SolicitudRepuestoDao

    private val ahora = Instant.parse("2026-11-07T10:00:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.solicitudRepuestoDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun las_de_una_incidencia_salen_de_la_mas_reciente_a_la_mas_antigua() = runTest {
        dao.guardar(solicitud("a", minutos = 1))
        dao.guardar(solicitud("b", minutos = 5))
        dao.guardar(solicitud("c", incidenciaId = "otra"))

        assertEquals(listOf("b", "a"), dao.observarPorIncidencia("i124").first().map { it.id })   // RF-1120
    }

    @Test
    fun cada_tecnico_ve_solo_las_suyas() = runTest {
        dao.guardar(solicitud("de-luis"))
        dao.guardar(solicitud("de-ana", tecnicoId = "demo-atorres"))

        assertEquals(listOf("de-luis"), dao.observarDeTecnico("demo-lramirez").first().map { it.id })   // RF-1121
    }

    @Test
    fun sin_numero_de_logistica_salen_las_no_registradas_y_no_las_rechazadas() = runTest {
        dao.guardar(solicitud("por-enviar"))
        dao.guardar(solicitud("registrada", numero = "17"))
        dao.guardar(solicitud("rechazada", rechazada = true))

        assertEquals(listOf("por-enviar"), dao.sinNumeroDeLogistica().map { it.id })   // RF-1117, RF-1119
    }

    @Test
    fun marcar_registrada_guarda_el_numero_y_la_deja_por_subir() = runTest {
        dao.guardar(solicitud("a", pendiente = false))

        dao.marcarRegistrada("a", "17", ahora)

        val guardada = dao.porId("a")!!
        assertEquals("17", guardada.numeroLogistica)
        assertTrue(guardada.pendienteSincronizar)
        assertTrue(dao.sinNumeroDeLogistica().isEmpty())
    }

    @Test
    fun marcar_rechazada_deja_de_reintentarla() = runTest {
        dao.guardar(solicitud("a"))

        dao.marcarRechazada("a", ahora)

        assertTrue(dao.porId("a")!!.rechazadaPorLogistica)
        assertTrue(dao.sinNumeroDeLogistica().isEmpty())   // RF-1119
    }

    @Test
    fun sin_entregar_del_tecnico_salen_las_registradas_que_no_llegaron() = runTest {
        dao.guardar(solicitud("en-camino", numero = "1", estado = EstadoEntrega.EN_CAMINO))
        dao.guardar(solicitud("entregada", numero = "2", estado = EstadoEntrega.ENTREGADO))
        dao.guardar(solicitud("por-enviar"))
        dao.guardar(solicitud("de-ana", numero = "3", tecnicoId = "demo-atorres"))

        assertEquals(listOf("en-camino"), dao.sinEntregarDe("demo-lramirez").map { it.id })   // RF-1123
    }

    @Test
    fun contar_sin_entregar_de_una_incidencia_no_cuenta_entregadas_ni_rechazadas() = runTest {
        dao.guardar(solicitud("a", numero = "1", estado = EstadoEntrega.EN_CAMINO))
        dao.guardar(solicitud("b", numero = "2", estado = EstadoEntrega.ENTREGADO))
        dao.guardar(solicitud("c", rechazada = true))
        dao.guardar(solicitud("d"))   // por enviar: todavía falta

        assertEquals(2, dao.contarSinEntregarDe("i124"))   // RF-1124
    }

    private fun solicitud(
        id: String,
        incidenciaId: String = "i124",
        tecnicoId: String = "demo-lramirez",
        numero: String? = null,
        estado: EstadoEntrega = EstadoEntrega.POR_DESPACHAR,
        rechazada: Boolean = false,
        pendiente: Boolean = true,
        minutos: Long = 0
    ) = SolicitudRepuestoEntity(
        id = id,
        incidenciaId = incidenciaId,
        pieza = "Fuente de poder ATX 500 W",
        motivo = "No entrega energía",
        fechaSolicitud = ahora.plusSeconds(minutos * 60),
        confirmadoEn = ahora.plusSeconds(minutos * 60),
        estadoEntrega = estado,
        solicitadoPorId = tecnicoId,
        solicitadoPorNombre = "Luis Ramírez",
        numeroLogistica = numero,
        stockConsultado = 3,
        diasEnvio = 1,
        despachadaEn = null,
        entregadaEn = null,
        llegadaEstimada = null,
        rechazadaPorLogistica = rechazada,
        pendienteSincronizar = pendiente,
        actualizadoEn = ahora
    )
}
