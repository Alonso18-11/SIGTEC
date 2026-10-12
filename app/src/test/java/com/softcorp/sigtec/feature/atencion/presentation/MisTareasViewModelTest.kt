package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.domain.ObservarBandeja
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-08 (RF-801, RF-803, RF-805): estado de la bandeja del técnico («Mis tareas»). */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MisTareasViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()

    // INC-0125: la más reciente de las dos
    private val masReciente = Incidencia(
        id = "i125", numero = 125, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-12T08:00:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.ASIGNADA, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    // INC-0124: la más antigua de las dos
    private val masAntigua = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "José Díaz", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private suspend fun TestScope.nuevoVm(): MisTareasViewModel {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = MisTareasViewModel(ObservarBandeja(ObservarUsuarioActual(sesion), repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `la bandeja llega ordenada de la mas antigua a la mas reciente`() = runTest {
        // RF-801
        repo.cargar(listOf(masReciente, masAntigua))
        val vm = nuevoVm()

        assertEquals(listOf(masAntigua, masReciente), vm.estado.value.incidencias)
    }

    @Test
    fun `sin pendientes la bandeja queda vacia`() = runTest {
        // RF-803
        repo.cargar(emptyList())
        val vm = nuevoVm()

        assertTrue(vm.estado.value.incidencias.isEmpty())
    }

    @Test
    fun `ahora avanza cada minuto sin que se vuelvan a cargar los datos`() = runTest {
        // RF-805, mismo patrón que AsignacionViewModelTest
        repo.cargar(listOf(masReciente, masAntigua))
        val vm = nuevoVm()
        val primera = vm.estado.value.ahora
        val incidenciasAntes = vm.estado.value.incidencias

        // El reloj usa Instant.now() real: una espera breve asegura que avance antes de adelantar
        // el scheduler de Main un minuto.
        Thread.sleep(50)
        main.dispatcher.scheduler.advanceTimeBy(60_000L)
        main.dispatcher.scheduler.runCurrent()

        assertNotEquals(primera, vm.estado.value.ahora)
        assertEquals(incidenciasAntes, vm.estado.value.incidencias)
    }
}
