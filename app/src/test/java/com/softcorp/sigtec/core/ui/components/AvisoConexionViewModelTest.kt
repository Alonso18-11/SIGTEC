package com.softcorp.sigtec.core.ui.components

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.domain.usecase.DescartarCambiosRechazados
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** HU-05 (RF-76, RF-80): el aviso refleja los rechazos y «Entendido» los descarta. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AvisoConexionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val estadoDatos = EstadoDatosRepositoryFalso()

    // Se crea dentro de cada prueba, cuando MainDispatcherRule ya reemplazó Dispatchers.Main
    private fun TestScope.nuevoVm(): AvisoConexionViewModel {
        val vm = AvisoConexionViewModel(ObservarEstadoDatos(estadoDatos), DescartarCambiosRechazados(estadoDatos))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `entendido descarta los rechazos y el aviso deja de mostrarlos`() = runTest {
        val vm = nuevoVm()
        estadoDatos.fijarNecesitanAtencion(2)
        assertEquals(2, vm.estado.value?.necesitanAtencion)

        vm.alEntendido()

        assertEquals(1, estadoDatos.descartes)
        assertEquals(0, vm.estado.value?.necesitanAtencion)
    }

    @Test
    fun `si descartar falla el aviso sigue mostrando los rechazos`() = runTest {
        val vm = nuevoVm()
        estadoDatos.fijarNecesitanAtencion(1)
        estadoDatos.fallar = true

        vm.alEntendido()

        assertEquals(1, vm.estado.value?.necesitanAtencion)
    }
}
