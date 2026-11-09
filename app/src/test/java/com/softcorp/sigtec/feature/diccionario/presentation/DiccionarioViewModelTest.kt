package com.softcorp.sigtec.feature.diccionario.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.diccionario.data.DiccionarioRepositoryFalso
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba
import com.softcorp.sigtec.feature.diccionario.domain.ObservarDiccionario
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** HU-13 (RF-1301 a RF-1309): el diccionario según el perfil, con búsqueda, filtro y fallas desplegadas. */
class DiccionarioViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = DiccionarioRepositoryFalso(FallasDePrueba.todas)

    private suspend fun crear(correo: String): DiccionarioViewModel {
        sesion.iniciarSesion(correo, "")
        return DiccionarioViewModel(ObservarDiccionario(VerificarPermiso(sesion), repo), main.dispatcher)
    }

    @Test
    fun `el tecnico ve todas ordenadas y puede registrar`() = runTest {
        val vm = crear("lramirez@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        val e = vm.estado.value
        assertFalse(e.cargando)
        assertEquals(4, e.total)
        assertEquals(FallasDePrueba.todas.sortedBy { it.titulo }, e.fallas)
        assertTrue(e.puedeRegistrar)
    }

    @Test
    fun `el personal de sistemas consulta sin registrar y el jefe no tiene acceso`() = runTest {
        val sistemas = crear("pcardenas@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sistemas.estado.collect {} }
        assertFalse(sistemas.estado.value.puedeRegistrar)
        assertEquals(4, sistemas.estado.value.total)

        val jefe = crear("cmendoza@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { jefe.estado.collect {} }
        assertTrue(jefe.estado.value.sinPermiso)
        assertTrue(jefe.estado.value.fallas.isEmpty())
    }

    @Test
    fun `buscar y filtrar se combinan sin cambiar el total`() = runTest {
        val vm = crear("lramirez@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        vm.alFiltrarTipo(TipoFalla.SOFTWARE)
        assertEquals(2, vm.estado.value.fallas.size)
        vm.alBuscar("controlador")
        assertEquals(listOf(FallasDePrueba.pantallaAzul), vm.estado.value.fallas)
        assertEquals(4, vm.estado.value.total)
        assertTrue(vm.estado.value.hayFiltro)

        vm.alFiltrarTipo(null)
        vm.alBuscar("")
        assertEquals(4, vm.estado.value.fallas.size)
    }

    @Test
    fun `una falla nueva que llega por la sincronizacion aparece sola`() = runTest {
        val vm = crear("lramirez@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        repo.agregar(FallasDePrueba.falla("f9", "Mouse con doble clic", "Mouse", "Hace doble clic solo"))

        assertEquals(5, vm.estado.value.total)
    }

    @Test
    fun `tocar una falla la despliega y tocarla otra vez la pliega`() = runTest {
        val vm = crear("lramirez@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        vm.alTocarFalla("f2")
        assertEquals("f2", vm.estado.value.desplegadaId)
        vm.alTocarFalla("f2")
        assertNull(vm.estado.value.desplegadaId)
    }
}
