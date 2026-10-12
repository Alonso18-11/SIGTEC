package com.softcorp.sigtec.feature.asignacion.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.asignacion.data.AsignacionRepositoryFalso
import com.softcorp.sigtec.feature.asignacion.domain.AsignarIncidencia
import com.softcorp.sigtec.feature.asignacion.domain.ObservarOpcionesAsignacion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-06 (RF-605, RF-607, RF-612, RF-616, RF-617): estado y eventos de la pantalla de asignación. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AsignacionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = AsignacionRepositoryFalso()

    private val luis = Tecnico("demo-lramirez", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
    private val ana = Tecnico("demo-atorres", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 5)

    private val incidencia = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    private suspend fun TestScope.nuevoVm(inc: Incidencia = incidencia): AsignacionViewModel {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(listOf(inc), listOf(luis, ana))
        val vm = AsignacionViewModel(
            SavedStateHandle(mapOf(AsignacionViewModel.ARG_INCIDENCIA_ID to inc.id)),
            ObservarOpcionesAsignacion(repo),
            AsignarIncidencia(VerificarPermiso(sesion), repo)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `sin eleccion no se puede asignar y al elegir uno con cupo si`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.estado.value.puedeAsignar)

        vm.alElegir(luis.id)

        assertEquals("Luis Ramírez", vm.estado.value.elegido?.tecnico?.nombre)
        assertTrue(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `un tecnico sin cupo no se puede elegir`() = runTest {
        // RF-603: Ana está en 5 de 5
        val vm = nuevoVm()
        vm.alElegir(ana.id)
        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `por sincronizar muestra el motivo y no deja asignar`() = runTest {
        // RF-607, RF-610
        val vm = nuevoVm(incidencia.copy(numero = null))
        vm.alElegir(luis.id)
        assertEquals(ErrorDominio.IncidenciaPorSincronizar, vm.estado.value.opciones?.bloqueo)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `al asignar emite el evento una sola vez y no permite guardar dos veces`() = runTest {
        // RF-617 y sin registro doble
        val vm = nuevoVm()
        vm.alElegir(luis.id)

        vm.alAsignar()
        vm.alAsignar()

        assertEquals("Luis Ramírez", vm.estado.value.asignadaA)
        assertTrue(vm.estado.value.guardando)
        assertEquals(1, repo.movimientos.size)
        vm.alNavegarAlDetalle()
        assertNull(vm.estado.value.asignadaA)
    }

    @Test
    fun `si el elegido se queda sin cupo la eleccion se borra`() = runTest {
        // RF-602: otra asignación suya, guardada en este celular, todavía no subió
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        repo.fijarSinSubir(mapOf(luis.id to 2))
        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `si Room falla muestra el error y se puede reintentar`() = runTest {
        // RF-616
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        repo.fallar = true

        vm.alAsignar()

        assertTrue(vm.estado.value.error is ErrorDominio.Desconocido)
        assertFalse(vm.estado.value.guardando)
        assertNull(vm.estado.value.asignadaA)
        assertTrue(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `con tecnico actual es reasignacion y el actual no se puede elegir`() = runTest {
        // RF-604, RF-605
        val asignadaALuis = incidencia.copy(
            marca = MarcaSeguimiento.ASIGNADA, tecnicoAsignadoId = luis.id, tecnicoAsignadoNombre = luis.nombre
        )
        val vm = nuevoVm(asignadaALuis)
        assertTrue(vm.estado.value.esReasignacion)

        vm.alElegir(luis.id)

        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }
}
