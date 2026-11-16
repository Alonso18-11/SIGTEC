package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.core.sync.TipoTareaDiaria
import com.softcorp.sigtec.feature.atencion.domain.ActualizarEstadoRepuestos
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** HU-18 (RF-1806 a RF-1808): la tarea diaria de repuestos usa la consulta de la HU-11. */
class TareaRepuestosTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val logistica = LogisticaRepositoryFalso()
    private val tarea = TareaRepuestos(ActualizarEstadoRepuestos(VerificarPermiso(sesion), repo, logistica), repo)
    private val ahora = Instant.parse("2026-11-16T13:05:00Z")
    private val zona = ZoneId.of("America/Lima")

    private fun solicitud(numero: String) = SolicitudRepuesto(
        id = "s$numero", incidenciaId = "i124", pieza = "Fuente de poder", motivo = "No enciende",
        fechaSolicitud = Instant.parse("2026-11-10T10:00:00Z"), confirmadoEn = Instant.parse("2026-11-10T10:00:00Z"),
        estadoEntrega = EstadoEntrega.POR_DESPACHAR, solicitadoPorId = "demo-lramirez",
        solicitadoPorNombre = "Luis Ramírez", numeroLogistica = numero
    )

    @Test
    fun `es la tarea de repuestos de las 8 y no necesita bajar datos`() {
        assertEquals(TipoTareaDiaria.REPUESTOS, tarea.tipo)
        assertFalse(tarea.necesitaDatosAlDia)
    }

    @Test
    fun `corre solo para quien atiende incidencias`() {
        assertTrue(tarea.correPara(Perfil.TECNICO))
        assertFalse(tarea.correPara(Perfil.JEFE))
        assertFalse(tarea.correPara(Perfil.PERSONAL_SISTEMAS))
    }

    @Test
    fun `con todas las consultas correctas termina y queda como la ultima exitosa`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertNull(tarea.ultimaExitosa())

        assertTrue(tarea.ejecutar(ahora, zona))
        assertEquals(ahora, tarea.ultimaExitosa())   // RF-1807: la misma anotación que la consulta a mano
    }

    @Test
    fun `si Logistica no responde no termina y se reintenta luego`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargarSolicitudes(listOf(solicitud("17")))
        logistica.fallaConsulta = ErrorLogistica.SIN_RESPUESTA

        assertFalse(tarea.ejecutar(ahora, zona))   // RF-1808
        assertNull(tarea.ultimaExitosa())
    }

    @Test
    fun `sin sesion no termina`() = runTest {
        assertFalse(tarea.ejecutar(ahora, zona))
    }
}
