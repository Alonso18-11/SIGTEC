package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.indicadores.data.HistorialRepositoryFalso
import com.softcorp.sigtec.feature.indicadores.domain.ObservarHistorialEquipo
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

/** HU-16 (RF-1601 a RF-1605): el estado del historial con código, con búsqueda, con escaneo y sin permiso. */
class HistorialEquipoViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = HistorialRepositoryFalso()

    private suspend fun TestScope.crear(
        codigo: String?,
        correo: String? = "lramirez@softcorp.pe",
        handle: SavedStateHandle = SavedStateHandle(mapOf(HistorialEquipoViewModel.ARG_CODIGO_EQUIPO to codigo))
    ): HistorialEquipoViewModel {
        correo?.let { sesion.iniciarSesion(it, "") }
        val vm = HistorialEquipoViewModel(handle, ObservarHistorialEquipo(VerificarPermiso(sesion), repo, main.dispatcher))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private fun incidencia(id: String, equipo: String) = Incidencia(
        id = id, numero = 7, codigoEquipo = equipo, descripcion = "No enciende", usuarioResponsable = "María Quispe",
        fechaRegistro = Instant.now(), registradoPorId = "u", registradoPorNombre = "Carlos Mendoza",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `con codigo en la ruta muestra su historial sin pasar por la busqueda`() = runTest {
        repo.datos.value = repo.datos.value.copy(incidencias = listOf(incidencia("i1", "PC-CONT-014")))
        val vm = crear("PC-CONT-014")

        val e = vm.estado.value
        assertFalse(e.modoBusqueda)
        assertFalse(e.cargando)
        assertEquals("PC-CONT-014", e.codigo)
        assertEquals(listOf("i1"), e.historial?.atenciones?.map { it.incidenciaId })
        assertFalse(e.puedeVolverABuscar)
    }

    @Test
    fun `sin codigo muestra la busqueda con el boton deshabilitado hasta escribir`() = runTest {
        val vm = crear(null)

        assertTrue(vm.estado.value.modoBusqueda)
        assertNull(vm.estado.value.codigo)
        assertFalse(vm.estado.value.puedeBuscar)

        vm.alCambiarTexto("   ")
        assertFalse(vm.estado.value.puedeBuscar)
        vm.alBuscar()
        assertNull(vm.estado.value.codigo)
    }

    @Test
    fun `buscar normaliza el codigo y volver regresa al formulario con el texto`() = runTest {
        repo.datos.value = repo.datos.value.copy(incidencias = listOf(incidencia("i1", "PC-LOG-009")))
        val vm = crear(null)

        vm.alCambiarTexto(" pc-log-009 ")
        assertTrue(vm.estado.value.puedeBuscar)
        vm.alBuscar()

        assertEquals("PC-LOG-009", vm.estado.value.codigo)
        assertEquals(listOf("i1"), vm.estado.value.historial?.atenciones?.map { it.incidenciaId })
        assertTrue(vm.estado.value.puedeVolverABuscar)

        vm.alVolverABuscar()
        assertNull(vm.estado.value.codigo)
        assertNull(vm.estado.value.historial)
        assertEquals("PC-LOG-009", vm.estado.value.texto)
    }

    @Test
    fun `el codigo escaneado se pone en el campo y se busca solo`() = runTest {
        val vm = crear(null)

        vm.alUsarCodigoEscaneado("PC-SIS-003")

        assertEquals("PC-SIS-003", vm.estado.value.texto)
        assertEquals("PC-SIS-003", vm.estado.value.codigo)
        assertEquals(listOf("PC-SIS-003"), repo.codigosPedidos)
    }

    @Test
    fun `el codigo buscado sobrevive a la recreacion con el mismo SavedStateHandle`() = runTest {
        val handle = SavedStateHandle(mapOf(HistorialEquipoViewModel.ARG_CODIGO_EQUIPO to null))
        crear(null, handle = handle).alUsarCodigoEscaneado("PC-SIS-003")

        val otra = crear(null, correo = null, handle = handle)

        assertEquals("PC-SIS-003", otra.estado.value.codigo)
        assertEquals("PC-SIS-003", otra.estado.value.texto)
    }

    @Test
    fun `sin sesion muestra el error sin permiso`() = runTest {
        val vm = crear("PC-CONT-014", correo = null)

        assertEquals(ErrorDominio.SinPermiso, vm.estado.value.error)
        assertNull(vm.estado.value.historial)
    }

    @Test
    fun `una incidencia nueva del equipo aparece sin volver a buscar`() = runTest {
        val vm = crear("PC-CONT-014")
        assertEquals(0, vm.estado.value.historial?.atenciones?.size)

        repo.datos.value = repo.datos.value.copy(incidencias = listOf(incidencia("i9", "PC-CONT-014")))

        assertEquals(listOf("i9"), vm.estado.value.historial?.atenciones?.map { it.incidenciaId })
    }
}
