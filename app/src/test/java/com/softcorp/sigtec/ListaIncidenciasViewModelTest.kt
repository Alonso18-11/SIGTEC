package com.softcorp.sigtec

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.ObservarIncidencias
import com.softcorp.sigtec.feature.incidencias.presentation.ListaIncidenciasUiState
import com.softcorp.sigtec.feature.incidencias.presentation.ListaIncidenciasViewModel
import com.softcorp.sigtec.feature.incidencias.presentation.SIN_TECNICO
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

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
    fun `busca por codigo INC, con o sin el prefijo`() = runTest {
        val vm = nuevoVm()
        vm.alBuscar("inc-0124")
        assertEquals(listOf("c"), vm.ids)
        vm.alBuscar(" 126 ")
        assertEquals(listOf("d"), vm.ids)
    }

    @Test
    fun `busca por equipo sin importar mayusculas`() = runTest {
        val vm = nuevoVm()
        vm.alBuscar("pc-cont")
        assertEquals(listOf("c", "e"), vm.ids)
    }

    @Test
    fun `busca por problema sin importar tildes`() = runTest {
        val vm = nuevoVm()
        vm.alBuscar("despues del CORTE")
        assertEquals(listOf("c"), vm.ids)
        vm.alBuscar("conexion")
        assertEquals(listOf("e"), vm.ids)
    }

    @Test
    fun `filtra por cada marca de seguimiento`() = runTest {
        val vm = nuevoVm()
        val esperado = mapOf(
            MarcaSeguimiento.SIN_ASIGNAR to listOf("b"),
            MarcaSeguimiento.ASIGNADA to listOf("e"),
            MarcaSeguimiento.EN_ATENCION to listOf("c"),
            MarcaSeguimiento.ESPERANDO_REPUESTO to listOf("d"),
            MarcaSeguimiento.CERRADA to listOf("a")
        )
        esperado.forEach { (marca, ids) ->
            vm.alFiltrarMarca(marca)
            assertEquals("marca $marca", ids, vm.ids)
        }
    }

    @Test
    fun `filtra por tecnico y por sin tecnico`() = runTest {
        val vm = nuevoVm()
        vm.alFiltrarTecnico("Ana Torres")
        assertEquals(listOf("d", "e"), vm.ids)
        vm.alFiltrarTecnico(SIN_TECNICO)
        assertEquals(listOf("b"), vm.ids)
    }

    @Test
    fun `filtra por equipo`() = runTest {
        val vm = nuevoVm()
        vm.alFiltrarEquipo("PC-CONT-014")
        assertEquals(listOf("c", "e"), vm.ids)
    }

    @Test
    fun `combina busqueda y filtros`() = runTest {
        val vm = nuevoVm()
        vm.alFiltrarEquipo("PC-CONT-014")
        vm.alFiltrarTecnico("Ana Torres")
        assertEquals(listOf("e"), vm.ids)
        vm.alBuscar("enciende")
        assertTrue(vm.ids.isEmpty())
        assertEquals(5, vm.ui.total)
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
}
