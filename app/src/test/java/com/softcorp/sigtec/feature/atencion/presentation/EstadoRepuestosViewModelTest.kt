package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.data.LogisticaRepositoryFalso
import com.softcorp.sigtec.feature.atencion.domain.ActualizarEstadoRepuestos
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import com.softcorp.sigtec.feature.atencion.domain.EstadoEnLogistica
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1121 a RF-1123): filtros, consulta al abrir y al deslizar, y lo último guardado sin conexión. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EstadoRepuestosViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val logistica = LogisticaRepositoryFalso()
    private val t0 = Instant.parse("2026-11-07T10:00:00Z")

    private val incidencia = Incidencia(
        id = "i116", numero = 116, codigoEquipo = "PC-LOG-009", descripcion = "El monitor parpadea",
        usuarioResponsable = "Juan Paredes", fechaRegistro = t0, registradoPorId = "demo-pcardenas",
        registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.ESPERANDO_REPUESTO,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun solicitud(id: String, estado: EstadoEntrega, minutos: Long, tecnico: String = "demo-lramirez") = SolicitudRepuesto(
        id = id, incidenciaId = "i116", pieza = "Pieza $id", motivo = "Motivo",
        fechaSolicitud = t0.plusSeconds(minutos * 60), confirmadoEn = t0.plusSeconds(minutos * 60),
        estadoEntrega = estado, solicitadoPorId = tecnico, solicitadoPorNombre = "Luis Ramírez",
        numeroLogistica = "n$id"
    )

    private suspend fun TestScope.nuevoVm(): EstadoRepuestosViewModel {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(incidencia))
        repo.cargarSolicitudes(
            listOf(
                solicitud("a", EstadoEntrega.EN_CAMINO, 1),
                solicitud("b", EstadoEntrega.ENTREGADO, 5),
                solicitud("c", EstadoEntrega.POR_DESPACHAR, 9),
                solicitud("de-ana", EstadoEntrega.POR_DESPACHAR, 2, tecnico = "demo-atorres")
            )
        )
        val vm = EstadoRepuestosViewModel(
            ObservarUsuarioActual(sesion),
            repo,
            ActualizarEstadoRepuestos(VerificarPermiso(sesion), repo, logistica)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val EstadoRepuestosViewModel.ui get() = estado.value

    @Test
    fun `muestra solo las del tecnico de la mas reciente a la mas antigua con su incidencia`() = runTest {
        val vm = nuevoVm()

        assertEquals(listOf("c", "b", "a"), vm.ui.solicitudes.map { it.solicitud.id })   // RF-1121
        assertEquals("INC-0116", vm.ui.solicitudes.first().codigoIncidencia)
        assertEquals("PC-LOG-009", vm.ui.solicitudes.first().codigoEquipo)
    }

    @Test
    fun `los filtros separan pendientes y entregados`() = runTest {
        val vm = nuevoVm()

        vm.alCambiarFiltro(FiltroRepuestos.PENDIENTES)
        assertEquals(listOf("c", "a"), vm.ui.solicitudes.map { it.solicitud.id })

        vm.alCambiarFiltro(FiltroRepuestos.ENTREGADOS)
        assertEquals(listOf("b"), vm.ui.solicitudes.map { it.solicitud.id })
    }

    @Test
    fun `al abrir consulta a Logistica y guarda lo que cambio`() = runTest {
        logistica.estados["na"] = EstadoEnLogistica(EstadoEntrega.ENTREGADO, null, Instant.parse("2026-11-08T10:00:00Z"), null)

        val vm = nuevoVm()

        assertEquals(EstadoEntrega.ENTREGADO, repo.solicitudes.first { it.id == "a" }.estadoEntrega)   // RF-1123
        assertNotNull(vm.ui.ultimaConsulta)
        assertFalse(vm.ui.consultando)
        assertFalse(vm.ui.sinRespuesta)
    }

    @Test
    fun `sin respuesta avisa y conserva lo ultimo guardado`() = runTest {
        logistica.fallaConsulta = ErrorLogistica.SIN_RESPUESTA

        val vm = nuevoVm()

        assertTrue(vm.ui.sinRespuesta)
        assertNull(vm.ui.ultimaConsulta)
        assertEquals(3, vm.ui.solicitudes.size)
    }

    @Test
    fun `deslizar vuelve a consultar`() = runTest {
        val vm = nuevoVm()
        val antes = repo.consultas.size

        vm.alConsultar()

        assertEquals(antes + 1, repo.consultas.size)
    }
}
