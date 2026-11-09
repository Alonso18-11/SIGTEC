package com.softcorp.sigtec.feature.indicadores.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import com.softcorp.sigtec.feature.indicadores.data.IndicadoresRepositoryFalso
import com.softcorp.sigtec.feature.indicadores.domain.ObservarIndicadores
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-15 (RF-1501 a RF-1504): el estado del tablero según el perfil y los datos. */
class IndicadoresViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = IndicadoresRepositoryFalso()
    private val estadoDatos = EstadoDatosRepositoryFalso()

    private suspend fun crear(correo: String): IndicadoresViewModel {
        sesion.iniciarSesion(correo, "")
        return IndicadoresViewModel(
            ObservarIndicadores(VerificarPermiso(sesion), repo, main.dispatcher),
            ObservarUsuarioActual(sesion),
            ObservarEstadoDatos(estadoDatos)
        )
    }

    @Test
    fun `el jefe ve sus datos, su nombre y que no hay descargas todavia`() = runTest {
        val vm = crear("cmendoza@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        val e = vm.estado.value
        assertFalse(e.cargando)
        assertFalse(e.sinPermiso)
        assertEquals("Carlos Mendoza", e.nombre)
        assertEquals("Sin datos del servidor todavía", e.actualizado)
        assertEquals(0, e.indicadores?.pendientes)
    }

    @Test
    fun `una incidencia nueva que llega por la sincronizacion cambia los pendientes`() = runTest {
        val vm = crear("cmendoza@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        repo.datos.value = repo.datos.value.copy(
            incidencias = listOf(
                Incidencia(
                    id = "i1", numero = 1, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
                    usuarioResponsable = "María Quispe", fechaRegistro = Instant.now(), registradoPorId = "u",
                    registradoPorNombre = "Carlos Mendoza", marca = MarcaSeguimiento.SIN_ASIGNAR
                )
            )
        )

        assertEquals(1, vm.estado.value.indicadores?.pendientes)
    }

    @Test
    fun `un tecnico no tiene acceso`() = runTest {
        val vm = crear("lramirez@softcorp.pe")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

        assertTrue(vm.estado.value.sinPermiso)
        assertNull(vm.estado.value.indicadores)
    }
}
