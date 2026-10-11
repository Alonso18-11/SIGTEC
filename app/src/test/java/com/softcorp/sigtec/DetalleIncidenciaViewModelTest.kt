package com.softcorp.sigtec

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.presentation.DetalleIncidenciaViewModel
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
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetalleIncidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val t0 = Instant.parse("2026-09-27T14:15:00Z")

    // Asignada a Luis y en atención, como INC-0124 del prototipo
    private val incidencia = Incidencia(
        id = "c", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = t0, registradoPorId = "demo-lramirez",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    // Cargados desordenados: el detalle debe mostrarlos del más reciente al más antiguo
    private val movimientos = listOf(
        Movimiento("m1", "c", TipoMovimiento.REGISTRADA, "Luis Ramírez", t0),
        Movimiento("m3", "c", TipoMovimiento.ATENCION_INICIADA, "Luis Ramírez", t0.plusSeconds(35 * 60)),
        Movimiento("m2", "c", TipoMovimiento.ASIGNADA, "Carlos Mendoza", t0.plusSeconds(17 * 60)),
        Movimiento("x", "otra", TipoMovimiento.REGISTRADA, "Ana Torres", t0.plusSeconds(60 * 60))
    )

    private fun TestScope.nuevoVm(
        id: String = "c",
        inc: Incidencia = incidencia
    ): DetalleIncidenciaViewModel {
        val repo = IncidenciaRepositoryFalso().also { it.cargar(listOf(inc), movimientos) }
        val vm = DetalleIncidenciaViewModel(
            SavedStateHandle(mapOf(DetalleIncidenciaViewModel.ARG_INCIDENCIA_ID to id)),
            ObservarDetalleIncidencia(repo),
            ObservarUsuarioActual(sesion)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `la linea de tiempo muestra el movimiento mas reciente arriba`() = runTest {
        val vm = nuevoVm()
        val tipos = vm.estado.value.detalle!!.movimientos.map { it.tipo }
        assertEquals(
            listOf(TipoMovimiento.ATENCION_INICIADA, TipoMovimiento.ASIGNADA, TipoMovimiento.REGISTRADA),
            tipos
        )
    }

    @Test
    fun `el jefe asigna y el tecnico asignado atiende`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val jefe = nuevoVm().estado.value
        assertTrue(jefe.puedeAsignar)
        assertFalse(jefe.puedeAtender)

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val tecnicoAsignado = nuevoVm().estado.value
        assertFalse(tecnicoAsignado.puedeAsignar)
        assertTrue(tecnicoAsignado.puedeAtender)

        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        val sistemas = nuevoVm().estado.value
        assertFalse(sistemas.puedeAsignar)
        assertFalse(sistemas.puedeAtender)
    }

    @Test
    fun `otro tecnico no puede atender una incidencia que no es suya`() = runTest {
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        val ana = nuevoVm().estado.value
        assertFalse(ana.puedeAtender)
        assertFalse(ana.puedeAsignar)
    }

    @Test
    fun `una incidencia solucionada no se asigna ni se atiende`() = runTest {
        val solucionada = incidencia.copy(marca = MarcaSeguimiento.CERRADA)

        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertFalse(nuevoVm(inc = solucionada).estado.value.puedeAsignar)

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertFalse(nuevoVm(inc = solucionada).estado.value.puedeAtender)
    }

    @Test
    fun `un id que no existe se informa como no encontrada`() = runTest {
        val estado = nuevoVm(id = "no-existe").estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrada)
    }

    @Test
    fun `ahora avanza cada minuto sin que cambien el detalle ni los permisos del usuario`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVm()
        val primera = vm.estado.value.ahora
        val detalleAntes = vm.estado.value.detalle            // incluye los movimientos (línea de tiempo)
        val puedeAsignarAntes = vm.estado.value.puedeAsignar
        val puedeAtenderAntes = vm.estado.value.puedeAtender   // depende del usuario observado

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

        assertNotEquals(primera, vm.estado.value.ahora)   // el temporizador funciona sin datos nuevos
        assertEquals(detalleAntes, vm.estado.value.detalle)
        assertEquals(puedeAsignarAntes, vm.estado.value.puedeAsignar)
        assertEquals(puedeAtenderAntes, vm.estado.value.puedeAtender)
    }
}