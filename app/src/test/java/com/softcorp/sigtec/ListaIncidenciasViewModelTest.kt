package com.softcorp.sigtec

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.ObservarIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.SIN_TECNICO
import com.softcorp.sigtec.feature.incidencias.presentation.ListaIncidenciasUiState
import com.softcorp.sigtec.feature.incidencias.presentation.ListaIncidenciasViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ListaIncidenciasViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val ahora = Instant.parse("2026-09-27T15:00:00Z")

    private fun incidencia(
        id: String, numero: Int?, equipo: String, descripcion: String,
        marca: MarcaSeguimiento, tecnico: String?, horasAtras: Long
    ) = Incidencia(
        id = id, numero = numero, codigoEquipo = equipo, descripcion = descripcion,
        usuarioResponsable = "María Quispe", fechaRegistro = ahora.minus(Duration.ofHours(horasAtras)),
        registradoPorId = "t1", registradoPorNombre = "Luis Ramírez", marca = marca,
        tecnicoAsignadoId = tecnico?.let { "id-$it" }, tecnicoAsignadoNombre = tecnico
    )

    // Cargadas desordenadas a propósito: el orden lo pone la consulta
    private val datos = listOf(
        incidencia("a", 119, "PC-ADM-022", "Teclas del teclado que no responden", MarcaSeguimiento.CERRADA, "Luis Ramírez", 170),
        incidencia("b", null, "IMP-RRHH-002", "La impresora no aparece en la red", MarcaSeguimiento.SIN_ASIGNAR, null, 1),
        incidencia("c", 124, "PC-CONT-014", "No enciende después del corte de luz", MarcaSeguimiento.EN_ATENCION, "Luis Ramírez", 3),
        incidencia("d", 126, "PC-VENT-031", "Pantalla azul al iniciar Windows", MarcaSeguimiento.ESPERANDO_REPUESTO, "Ana Torres", 2),
        incidencia("e", 121, "PC-CONT-014", "Sin conexión a la red Wi-Fi", MarcaSeguimiento.ASIGNADA, "Ana Torres", 150)
    )

    private fun TestScope.nuevoVm(): ListaIncidenciasViewModel {
        val repo = IncidenciaRepositoryFalso().apply { cargar(datos) }
        val vm = ListaIncidenciasViewModel(ObservarIncidencias(repo))
        // WhileSubscribed necesita a alguien mirando; Unconfined lo pone a mirar de inmediato
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val ListaIncidenciasViewModel.ids: List<String>
        get() = estado.value.incidencias.map { it.id }

    private val ListaIncidenciasViewModel.ui: ListaIncidenciasUiState
        get() = estado.value

    @Test
    fun `sin filtros muestra todas de la mas reciente a la mas antigua`() = runTest {
        val vm = nuevoVm()
        assertEquals(listOf("b", "d", "c", "e", "a"), vm.ids)
        assertEquals(5, vm.ui.total)
        assertFalse(vm.ui.cargando)
    }

    @Test
    fun `limpiar quita la busqueda y todos los filtros de un toque`() = runTest {
        val vm = nuevoVm()
        vm.alBuscar("red")
        vm.alFiltrarMarca(MarcaSeguimiento.SIN_ASIGNAR)
        vm.alFiltrarTecnico(SIN_TECNICO)
        vm.alFiltrarEquipo("IMP-RRHH-002")
        assertTrue(vm.ui.filtros.activos)

        vm.limpiar()
        assertFalse(vm.ui.filtros.activos)
        assertEquals(listOf("b", "d", "c", "e", "a"), vm.ids)
    }

    @Test
    fun `las opciones de tecnico y equipo salen de los datos`() = runTest {
        val vm = nuevoVm()
        assertEquals(listOf(SIN_TECNICO, "Ana Torres", "Luis Ramírez"), vm.ui.opcionesTecnico)
        assertEquals(listOf("IMP-RRHH-002", "PC-ADM-022", "PC-CONT-014", "PC-VENT-031"), vm.ui.opcionesEquipo)
    }

    @Test
    fun `ahora avanza cada minuto sin que cambien las incidencias ni los filtros`() = runTest {
        val vm = nuevoVm()
        val primera = vm.ui.ahora
        val incidenciasAntes = vm.ids
        val filtrosAntes = vm.ui.filtros

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

        assertNotEquals(primera, vm.ui.ahora)       // el temporizador funciona sin datos nuevos
        assertEquals(incidenciasAntes, vm.ids)      // las incidencias no cambiaron
        assertEquals(filtrosAntes, vm.ui.filtros)   // los filtros no cambiaron
    }
}
