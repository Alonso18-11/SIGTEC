package com.softcorp.sigtec

import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.data.PreferenciasSeguridadFalsas
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.usecase.DesbloquearApp
import com.softcorp.sigtec.core.domain.usecase.EvaluarIngresoDeOtraPersona
import com.softcorp.sigtec.core.domain.usecase.IniciarSesion
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoAcceso
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.domain.usecase.ObservarPreferenciasHuella
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import com.softcorp.sigtec.feature.acceso.presentation.LoginUiState
import com.softcorp.sigtec.feature.acceso.presentation.LoginViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

// HU-01 (specs/001-inicio-sesion)
class LoginViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val preferencias = PreferenciasSeguridadFalsas()
    private val bloqueo = BloqueoApp()
    private val estadoDatos = EstadoDatosRepositoryFalso()

    private fun TestScope.nuevoVm(): LoginViewModel {
        val observarEstadoAcceso = ObservarEstadoAcceso(sesion, preferencias, bloqueo)
        val vm = LoginViewModel(
            IniciarSesion(sesion, bloqueo),
            DesbloquearApp(bloqueo),
            EvaluarIngresoDeOtraPersona(observarEstadoAcceso, ObservarEstadoDatos(estadoDatos)),
            observarEstadoAcceso,
            ObservarPreferenciasHuella(preferencias)
        )
        // WhileSubscribed necesita a alguien mirando; Unconfined lo pone a mirar de inmediato
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val LoginViewModel.ui: LoginUiState get() = estado.value

    @Test
    fun `con campos vacios no se puede ingresar ni se llama a la sesion`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.ui.puedeIngresar)

        vm.alCambiarCorreo("lramirez@softcorp.pe")
        assertFalse(vm.ui.puedeIngresar)   // falta la contraseña

        vm.ingresar()
        assertNull(sesion.usuarioActual.value)
        assertNull(vm.ui.error)
    }

    @Test
    fun `con correo y contrasena llenos se habilita el ingreso`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCorreo("lramirez@softcorp.pe")
        vm.alCambiarContrasena("clave")
        assertTrue(vm.ui.puedeIngresar)
    }

    @Test
    fun `credenciales correctas abren la sesion y desbloquean`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCorreo("  LRamirez@softcorp.pe ")   // se normaliza antes de validar
        vm.alCambiarContrasena("clave")

        vm.ingresar()

        assertEquals("demo-lramirez", sesion.usuarioActual.value?.id)
        assertTrue(bloqueo.desbloqueada.value)
        assertNull(vm.ui.error)
        assertFalse(vm.ui.cargando)
    }

    @Test
    fun `credenciales incorrectas muestran el error y no abren sesion`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCorreo("nadie@softcorp.pe")
        vm.alCambiarContrasena("clave")

        vm.ingresar()

        assertEquals(ErrorDominio.CredencialesInvalidas, vm.ui.error)
        assertNull(sesion.usuarioActual.value)
        assertFalse(vm.ui.cargando)
        assertTrue(vm.ui.puedeIngresar)   // puede corregir y reintentar
    }

    @Test
    fun `al corregir el correo o la contrasena se limpia el error`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCorreo("nadie@softcorp.pe")
        vm.alCambiarContrasena("clave")
        vm.ingresar()
        assertNotNull(vm.ui.error)

        vm.alCambiarCorreo("lramirez@softcorp.pe")
        assertNull(vm.ui.error)

        vm.ingresar()
        vm.alCambiarContrasena("otra")
        assertNull(vm.ui.error)
    }

    @Test
    fun `alternar visibilidad muestra y oculta la contrasena`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.ui.contrasenaVisible)
        vm.alternarVisibilidad()
        assertTrue(vm.ui.contrasenaVisible)
        vm.alternarVisibilidad()
        assertFalse(vm.ui.contrasenaVisible)
    }

    @Test
    fun `sin sesion pero con dueno de huella se muestra su nombre`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        val vm = nuevoVm()
        assertEquals("Luis Ramírez", vm.ui.propietarioHuella)
        assertNull(vm.ui.usuarioBloqueado)
    }

    @Test
    fun `el dueno bloqueado se desbloquea con su huella`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")   // sesión restaurada, sin desbloquear
        val vm = nuevoVm()
        assertEquals("demo-lramirez", vm.ui.usuarioBloqueado?.id)

        vm.alVerificarHuella(ResultadoBiometrico.Exito(Instant.now()))

        assertTrue(bloqueo.desbloqueada.value)
        assertNull(vm.ui.usuarioBloqueado)
    }

    @Test
    fun `si la huella falla se avisa y sigue bloqueado`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVm()

        vm.alVerificarHuella(ResultadoBiometrico.Error("Huella no reconocida"))

        assertEquals("Huella no reconocida", vm.ui.avisoHuella)
        assertFalse(bloqueo.desbloqueada.value)
    }

    @Test
    fun `cancelar la huella no cambia nada`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVm()

        vm.alVerificarHuella(ResultadoBiometrico.Cancelado)

        assertNull(vm.ui.avisoHuella)
        assertFalse(bloqueo.desbloqueada.value)
        assertNotNull(vm.ui.usuarioBloqueado)
    }

    @Test
    fun `al bloquearse el dueno se ofrece la huella una sola vez y no reaparece al recomponer`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        val vm = nuevoVm()
        assertNull(vm.ui.ofrecerHuellaParaId)   // sin sesión todavía, nada que ofrecer

        sesion.iniciarSesion("lramirez@softcorp.pe", "")   // transición real a Bloqueada

        assertEquals("demo-lramirez", vm.ui.ofrecerHuellaParaId)

        vm.alHuellaOfrecida()
        assertNull(vm.ui.ofrecerHuellaParaId)

        vm.alCambiarCorreo("x")   // fuerza otra recomposición del combine, sin un nuevo bloqueo
        assertNull(vm.ui.ofrecerHuellaParaId)   // RF-29: el evento ya consumido no reaparece solo
    }

    @Test
    fun `ingresar con otra cuenta y pendientes abre el dialogo y no inicia sesion`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")   // dueño bloqueado
        estadoDatos.fijarPendientes(2)
        val vm = nuevoVm()
        vm.alCambiarCorreo("atorres@softcorp.pe")
        vm.alCambiarContrasena("clave")

        vm.ingresar()

        assertEquals(2, vm.ui.confirmarIngreso)
        assertEquals("demo-lramirez", sesion.usuarioActual.value?.id)   // no se autenticó a la otra persona
        assertFalse(vm.ui.cargando)
    }

    @Test
    fun `confirmar el ingreso de otra cuenta si autentica`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        estadoDatos.fijarPendientes(2)
        val vm = nuevoVm()
        vm.alCambiarCorreo("atorres@softcorp.pe")
        vm.alCambiarContrasena("clave")
        vm.ingresar()
        assertNotNull(vm.ui.confirmarIngreso)

        vm.alConfirmarIngreso()

        assertNull(vm.ui.confirmarIngreso)
        assertEquals("demo-atorres", sesion.usuarioActual.value?.id)
        assertNull(vm.ui.error)
        assertFalse(vm.ui.cargando)
    }

    @Test
    fun `cancelar el ingreso de otra cuenta no autentica y cierra el dialogo`() = runTest {
        preferencias.asignarPropietario("demo-lramirez", "Luis Ramírez")
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        estadoDatos.fijarPendientes(2)
        val vm = nuevoVm()
        vm.alCambiarCorreo("atorres@softcorp.pe")
        vm.alCambiarContrasena("clave")
        vm.ingresar()
        assertNotNull(vm.ui.confirmarIngreso)

        vm.alCancelarIngreso()

        assertNull(vm.ui.confirmarIngreso)
        assertEquals("demo-lramirez", sesion.usuarioActual.value?.id)   // sigue siendo el dueño bloqueado
    }
}
