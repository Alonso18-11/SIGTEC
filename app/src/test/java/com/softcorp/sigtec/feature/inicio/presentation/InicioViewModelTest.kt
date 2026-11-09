package com.softcorp.sigtec.feature.inicio.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.inicio.domain.ObservarResumenInicio
import com.softcorp.sigtec.feature.inicio.domain.TipoMetrica
import com.softcorp.sigtec.feature.inicio.domain.VistaInicio
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** Inicio (RF-I1, RF-I4, RF-I5, RF-I11, RF-I12): el estado sigue a la sesión y a lo que hay en Room. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val incidencias = IncidenciaRepositoryFalso()
    private val atencion = AtencionRepositoryFalso()

    private fun TestScope.nuevoVm(): InicioViewModel {
        val vm = InicioViewModel(
            ObservarUsuarioActual(sesion),
            ObservarResumenInicio(ObservarUsuarioActual(sesion), incidencias, atencion)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private fun inc(id: String, marca: MarcaSeguimiento, tecnico: String? = null) = Incidencia(
        id = id, numero = 1, codigoEquipo = "PC-CONT-014", descripcion = "Falla", usuarioResponsable = "María",
        fechaRegistro = Instant.parse("2026-11-01T09:00:00Z"), registradoPorId = "demo-pcardenas",
        registradoPorNombre = "Pedro", marca = marca, tecnicoAsignadoId = tecnico
    )

    @Test
    fun `sin sesion no hay resumen`() = runTest {
        val vm = nuevoVm()

        assertNull(vm.estado.value.resumen)   // RF-I12
    }

    @Test
    fun `el tecnico ve su nombre, su cargo y sus metricas`() = runTest {
        incidencias.cargar(listOf(inc("a", MarcaSeguimiento.EN_ATENCION, "demo-lramirez"), inc("b", MarcaSeguimiento.ASIGNADA, "demo-atorres")))
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")

        val ui = vm.estado.value
        assertEquals("Luis", ui.primerNombre)
        assertTrue(ui.saludo.startsWith("Buen"))   // RF-I1, la hora depende del reloj real
        assertEquals(VistaInicio.TECNICO, ui.resumen?.vista)
        assertEquals(1, ui.resumen?.metricas?.first { it.tipo == TipoMetrica.EN_ATENCION }?.valor)
        assertEquals("a", ui.resumen?.atendiendo?.id)
    }

    @Test
    fun `el resumen se actualiza cuando cambian los datos`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertEquals(0, vm.estado.value.resumen?.metricas?.first { it.tipo == TipoMetrica.SIN_ASIGNAR }?.valor)

        incidencias.cargar(listOf(inc("n", MarcaSeguimiento.SIN_ASIGNAR)))

        assertEquals(1, vm.estado.value.resumen?.metricas?.first { it.tipo == TipoMetrica.SIN_ASIGNAR }?.valor)   // RF-I11
    }
}
