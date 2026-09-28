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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DetalleIncidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val t0 = Instant.parse("2026-09-27T14:15:00Z")

    private val incidencia = Incidencia(
        id = "c", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = t0, registradoPorId = "demo-lramirez",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION
    )

    // Cargados desordenados: el detalle debe mostrarlos del más reciente al más antiguo
    private val movimientos = listOf(
        Movimiento("m1", "c", TipoMovimiento.REGISTRADA, "Luis Ramírez", t0),
        Movimiento("m3", "c", TipoMovimiento.ATENCION_INICIADA, "Luis Ramírez", t0.plusSeconds(35 * 60)),
        Movimiento("m2", "c", TipoMovimiento.ASIGNADA, "Carlos Mendoza", t0.plusSeconds(17 * 60)),
        Movimiento("x", "otra", TipoMovimiento.REGISTRADA, "Ana Torres", t0.plusSeconds(60 * 60))
    )

    private fun TestScope.nuevoVm(id: String = "c"): DetalleIncidenciaViewModel {
        val repo = IncidenciaRepositoryFalso().also { it.cargar(listOf(incidencia), movimientos) }
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
    fun `solo el jefe ve asignar y solo el tecnico ve repuesto y cierre`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val jefe = nuevoVm().estado.value
        assertTrue(jefe.puedeAsignar)
        assertFalse(jefe.puedeAtender)

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val tecnico = nuevoVm().estado.value
        assertFalse(tecnico.puedeAsignar)
        assertTrue(tecnico.puedeAtender)

        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        val sistemas = nuevoVm().estado.value
        assertFalse(sistemas.puedeAsignar)
        assertFalse(sistemas.puedeAtender)
    }

    @Test
    fun `un id que no existe se informa como no encontrada`() = runTest {
        val estado = nuevoVm(id = "no-existe").estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrada)
    }
}
