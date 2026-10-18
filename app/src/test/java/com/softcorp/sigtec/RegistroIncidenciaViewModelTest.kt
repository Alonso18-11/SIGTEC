package com.softcorp.sigtec

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.RegistrarIncidencia
import com.softcorp.sigtec.feature.incidencias.presentation.RegistroIncidenciaUiState
import com.softcorp.sigtec.feature.incidencias.presentation.RegistroIncidenciaViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// HU-03 (specs/003-registro-incidencia)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RegistroIncidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = IncidenciaRepositoryFalso()

    private fun TestScope.nuevoVm(): RegistroIncidenciaViewModel {
        val vm = RegistroIncidenciaViewModel(
            RegistrarIncidencia(VerificarPermiso(sesion), repo),
            ObservarUsuarioActual(sesion)
        )
        // WhileSubscribed necesita a alguien mirando; Unconfined lo pone a mirar de inmediato
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val RegistroIncidenciaViewModel.ui: RegistroIncidenciaUiState get() = estado.value

    @Test
    fun `sin sesion o con un campo vacio el boton queda deshabilitado`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.ui.puedeGuardar)   // sin sesión y sin datos

        vm.alCambiarCodigo("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")
        assertFalse(vm.ui.puedeGuardar)   // formulario completo, pero todavía sin sesión

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertTrue(vm.ui.puedeGuardar)   // con sesión y los tres campos completos, ya se puede guardar

        vm.alCambiarResponsable("")
        assertFalse(vm.ui.puedeGuardar)   // un campo vuelve a quedar vacío
    }

    @Test
    fun `la fecha mostrada avanza cada minuto mientras la pantalla esta abierta`() = runTest {
        val vm = nuevoVm()
        val primera = vm.ui.fechaHora

        // El reloj del ViewModel usa Instant.now() real (no es inyectable, ver plan.md sección 1):
        // una espera real breve asegura que el reloj del sistema avance entre ambas lecturas,
        // porque su resolución puede ser más gruesa que el tiempo que tarda en ejecutarse esta prueba.
        Thread.sleep(50)

        // El delay() del reloj corre en viewModelScope (Dispatchers.Main), cuyo scheduler de prueba
        // es el de MainDispatcherRule, no el de este runTest; hay que avanzar ese scheduler, no el propio.
        // RELOJ_MS es privado en el ViewModel; 60_000L replica su valor (un minuto).
        // advanceTimeBy no ejecuta lo programado justo en el nuevo instante; runCurrent lo hace.
        main.dispatcher.scheduler.advanceTimeBy(60_000L)
        main.dispatcher.scheduler.runCurrent()

        val segunda = vm.ui.fechaHora
        assertNotEquals(primera, segunda)   // no se fija el valor exacto, solo que cambió
    }

    @Test
    fun `guardar con exito deja registradaId y alNavegarAlDetalle lo limpia`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")

        vm.guardar()
        // advanceUntilIdle() no sirve aquí: el reloj de fechaHora reprograma un delay() infinito
        // en el mismo scheduler de Main y nunca queda "idle"; runCurrent() basta porque guardar()
        // no depende de ningún delay().
        main.dispatcher.scheduler.runCurrent()

        val id = vm.ui.registradaId
        assertNotNull(id)                               // RF-40: evento de un solo uso
        assertEquals(repo.incidencias.single().id, id)   // coincide con el id real guardado

        vm.alNavegarAlDetalle()
        assertNull(vm.ui.registradaId)
    }

    @Test
    fun `guardar con un equipo fuera del catalogo deja el error y permite reintentar`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-14")   // fuera del catálogo falso, a diferencia de "PC-CONT-014"
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")

        vm.guardar()
        // advanceUntilIdle() no sirve aquí: el reloj de fechaHora reprograma un delay() infinito
        // en el mismo scheduler de Main y nunca queda "idle"; runCurrent() basta porque guardar()
        // no depende de ningún delay().
        main.dispatcher.scheduler.runCurrent()

        assertEquals(ErrorDominio.EquipoNoEncontrado, vm.ui.error)   // RF-41
        assertNull(vm.ui.registradaId)
        assertFalse(vm.ui.guardando)   // se puede reintentar
    }

    // HU-09 (specs/009-escaneo-evidencias)

    @Test
    fun `el codigo escaneado reemplaza el escrito y muestra que vino de la camara`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCodigo("PC-LOG-9")
        assertFalse(vm.ui.codigoLeidoConCamara)

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertEquals("PC-CONT-014", vm.ui.codigoEquipo)   // RF-910
        assertTrue(vm.ui.codigoLeidoConCamara)
    }

    @Test
    fun `editar a mano el codigo escaneado quita la indicacion de la camara`() = runTest {
        val vm = nuevoVm()
        vm.alUsarCodigoEscaneado("PC-CONT-014")

        vm.alCambiarCodigo("PC-CONT-01")

        assertEquals("PC-CONT-01", vm.ui.codigoEquipo)
        assertFalse(vm.ui.codigoLeidoConCamara)   // RF-911
    }

    @Test
    fun `recibir un codigo escaneado limpia el error anterior`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-14")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")
        vm.guardar()
        main.dispatcher.scheduler.runCurrent()
        assertEquals(ErrorDominio.EquipoNoEncontrado, vm.ui.error)

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertNull(vm.ui.error)
    }
}
