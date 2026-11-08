package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.data.LogisticaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1117, RF-1119): las solicitudes sin número se registran en Logística en segundo plano. */
class RegistrarSolicitudesEnLogisticaTest {

    private val repo = AtencionRepositoryFalso()
    private val logistica = LogisticaRepositoryFalso()
    private val registrar = RegistrarSolicitudesEnLogistica(repo, logistica)
    private val ahora = Instant.parse("2026-11-07T16:00:00Z")

    private val incidencia = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-03T09:15:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.ESPERANDO_REPUESTO, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun solicitud(id: String, numero: String? = null, rechazada: Boolean = false) = SolicitudRepuesto(
        id = id, incidenciaId = "i124", pieza = "Fuente de poder ATX 500 W", motivo = "No entrega energía",
        fechaSolicitud = ahora, confirmadoEn = ahora, estadoEntrega = EstadoEntrega.POR_DESPACHAR,
        solicitadoPorId = "demo-lramirez", solicitadoPorNombre = "Luis Ramírez",
        numeroLogistica = numero, rechazadaPorLogistica = rechazada
    )

    @Test
    fun `registra las solicitudes sin numero y guarda el que devuelve Logistica`() = runTest {
        repo.cargar(listOf(incidencia))
        repo.cargarSolicitudes(listOf(solicitud("a"), solicitud("b", numero = "7"), solicitud("c", rechazada = true)))

        val r = registrar(ahora)

        assertEquals(ResultadoRegistro(registradas = 1, rechazadas = 0, pendientes = 0), r)   // RF-1117
        assertEquals(listOf("a"), logistica.registradas.map { it.id })
        assertEquals("100", repo.solicitudes.first { it.id == "a" }.numeroLogistica)
    }

    @Test
    fun `sin respuesta de Logistica quedan pendientes para el proximo intento`() = runTest {
        repo.cargar(listOf(incidencia))
        repo.cargarSolicitudes(listOf(solicitud("a")))
        logistica.fallaRegistro = ErrorLogistica.SIN_RESPUESTA

        val r = registrar(ahora)

        assertEquals(1, r.pendientes)
        assertNull(repo.solicitudes.single().numeroLogistica)
    }

    @Test
    fun `si Logistica la rechaza deja de reintentarla`() = runTest {
        repo.cargar(listOf(incidencia))
        repo.cargarSolicitudes(listOf(solicitud("a")))
        logistica.fallaRegistro = ErrorLogistica.RECHAZADA

        val r = registrar(ahora)

        assertEquals(ResultadoRegistro(registradas = 0, rechazadas = 1, pendientes = 0), r)   // RF-1119
        assertTrue(repo.solicitudes.single().rechazadaPorLogistica)
        assertTrue(repo.solicitudesPorRegistrar().isEmpty())
    }

    @Test
    fun `si Room falla al guardar el numero queda pendiente`() = runTest {
        repo.cargar(listOf(incidencia))
        repo.cargarSolicitudes(listOf(solicitud("a")))
        repo.fallar = true

        assertEquals(1, registrar(ahora).pendientes)
    }

    @Test
    fun `sin nada por registrar no llama a Logistica`() = runTest {
        repo.cargarSolicitudes(listOf(solicitud("b", numero = "7")))

        assertEquals(ResultadoRegistro(0, 0, 0), registrar(ahora))
        assertTrue(logistica.registradas.isEmpty())
    }
}
