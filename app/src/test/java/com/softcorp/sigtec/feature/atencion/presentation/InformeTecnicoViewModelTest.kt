package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.domain.CerrarIncidenciaConInforme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-08 (RF-813 a RF-820): estado del formulario de «Informe técnico» y su confirmación biométrica. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InformeTecnicoViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val confirmadoEn = Instant.parse("2026-10-12T15:30:00Z")

    // INC-0127: asignada a Luis Ramírez, en atención
    private val enAtencion = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private suspend fun nuevoVm(
        disponibilidad: DisponibilidadBiometrica = DisponibilidadBiometrica.HUELLA,
        incidenciaId: String = "i127"
    ): InformeTecnicoViewModel {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        return InformeTecnicoViewModel(
            SavedStateHandle(mapOf("incidenciaId" to incidenciaId)),
            CerrarIncidenciaConInforme(VerificarPermiso(sesion), repo),
            disponibilidad
        )
    }

    @Test
    fun `con los campos vacios no se puede confirmar`() = runTest {
        // RF-814
        val vm = nuevoVm()
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun `con solo el diagnostico no se puede confirmar`() = runTest {
        // RF-814
        val vm = nuevoVm()
        vm.alCambiarDiagnostico("Fusor dañado")
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun `con solo la solucion aplicada no se puede confirmar`() = runTest {
        // RF-814
        val vm = nuevoVm()
        vm.alCambiarTrabajo("Se cambió el fusor")
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun `mientras se guarda no se puede confirmar de nuevo`() = runTest {
        // RF-814: un segundo toque no duplica el cierre
        val vm = nuevoVm()
        vm.alCambiarDiagnostico("Fusor dañado")
        vm.alCambiarTrabajo("Se cambió el fusor")

        vm.alConfirmarCierre(confirmadoEn)
        vm.alConfirmarCierre(confirmadoEn)

        assertTrue(vm.estado.value.guardando)
        assertFalse(vm.estado.value.puedeConfirmar)
        assertEquals(1, repo.informes.size)
    }

    @Test
    fun `sin bloqueo de pantalla no se puede confirmar aunque el formulario este completo`() = runTest {
        // RF-816
        val vm = nuevoVm(disponibilidad = DisponibilidadBiometrica.SIN_SEGURIDAD)
        vm.alCambiarDiagnostico("Fusor dañado")
        vm.alCambiarTrabajo("Se cambió el fusor")

        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun `al confirmar con exito emite el evento cerrada`() = runTest {
        // RF-819, RF-821
        val vm = nuevoVm()
        vm.alCambiarDiagnostico("Fusor dañado")
        vm.alCambiarTrabajo("Se cambió el fusor")

        vm.alConfirmarCierre(confirmadoEn)

        assertTrue(vm.estado.value.cerrada)
        val informe = repo.informes.single()
        assertEquals("Fusor dañado", informe.diagnostico)
        assertEquals(confirmadoEn, informe.confirmadoEn)
    }

    @Test
    fun `MarcaCambiada queda en error y no pierde el texto escrito`() = runTest {
        // RF-812 reflejado en el ViewModel
        val vm = nuevoVm()
        repo.cargar(listOf(enAtencion.copy(marca = MarcaSeguimiento.SIN_ASIGNAR)))
        vm.alCambiarDiagnostico("Fusor dañado")
        vm.alCambiarTrabajo("Se cambió el fusor")

        vm.alConfirmarCierre(confirmadoEn)

        assertEquals(ErrorDominio.MarcaCambiada, vm.estado.value.error)
        assertEquals("Fusor dañado", vm.estado.value.diagnostico)
        assertEquals("Se cambió el fusor", vm.estado.value.trabajoRealizado)
        assertFalse(vm.estado.value.guardando)
    }

    @Test
    fun `diagnostico con solo espacios deja el boton deshabilitado`() = runTest {
        // RF-818: puedeConfirmar usa isNotBlank(), igual que recortarCampos().isEmpty() tras
        // trim(); CampoObligatorio es la defensa del caso de uso para cuando, aun así, algo
        // lo invocara (no alcanzable desde este ViewModel porque el botón ya lo impide).
        val vm = nuevoVm()
        vm.alCambiarDiagnostico("   ")
        vm.alCambiarTrabajo("Se cambió el fusor")
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun `Desconocido queda en error y permite reintentar sin perder el texto`() = runTest {
        // RF-820
        val vm = nuevoVm()
        repo.fallar = true
        vm.alCambiarDiagnostico("Fusor dañado")
        vm.alCambiarTrabajo("Se cambió el fusor")

        vm.alConfirmarCierre(confirmadoEn)

        assertTrue(vm.estado.value.error is ErrorDominio.Desconocido)
        assertFalse(vm.estado.value.guardando)
        assertEquals("Fusor dañado", vm.estado.value.diagnostico)
        assertEquals("Se cambió el fusor", vm.estado.value.trabajoRealizado)
        assertTrue(vm.estado.value.puedeConfirmar)

        // Reintentar sin perder el texto (D-8: hay que volver a confirmar con la huella)
        repo.fallar = false
        vm.alConfirmarCierre(confirmadoEn)
        assertTrue(vm.estado.value.cerrada)
    }
}
