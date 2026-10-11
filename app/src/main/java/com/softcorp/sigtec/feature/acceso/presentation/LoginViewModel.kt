package com.softcorp.sigtec.feature.acceso.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.usecase.DesbloquearApp
import com.softcorp.sigtec.core.domain.usecase.EvaluarIngresoDeOtraPersona
import com.softcorp.sigtec.core.domain.usecase.IniciarSesion
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoAcceso
import com.softcorp.sigtec.core.domain.usecase.ObservarPreferenciasHuella
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: ErrorDominio? = null,
    val usuarioBloqueado: Usuario? = null,      // el dueño, esperando su huella
    val propietarioHuella: String? = null,      // sin sesión, pero el celular tiene dueño de huella
    val avisoHuella: String? = null,
    // Evento de un solo uso: null = sin diálogo, N = mostrar diálogo con N pendientes del dueño bloqueado. // RF-11
    val confirmarIngreso: Int? = null,
    // Evento de un solo uso: null = nada que ofrecer, el id = lanzar la huella una vez. // RF-29
    val ofrecerHuellaParaId: String? = null
) {
    val puedeIngresar: Boolean
        get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesion,
    private val desbloquearApp: DesbloquearApp,
    private val evaluarIngresoDeOtraPersona: EvaluarIngresoDeOtraPersona,
    observarEstadoAcceso: ObservarEstadoAcceso,
    observarPreferenciasHuella: ObservarPreferenciasHuella
) : ViewModel() {

    private val formulario = MutableStateFlow(LoginUiState())

    val estado: StateFlow<LoginUiState> = combine(
        formulario, observarEstadoAcceso(), observarPreferenciasHuella()
    ) { f, acceso, prefs ->
        f.copy(
            usuarioBloqueado = (acceso as? EstadoAcceso.Bloqueada)?.usuario,
            propietarioHuella = if (acceso is EstadoAcceso.SinSesion) prefs.propietarioNombre else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoginUiState())

    init {
        // RF-29: ofrece la huella solo ante una transición real a Bloqueada, no en cada recomposición del combine.
        viewModelScope.launch {
            observarEstadoAcceso()
                .map { (it as? EstadoAcceso.Bloqueada)?.usuario?.id }
                .distinctUntilChanged()
                .collect { idBloqueado ->
                    if (idBloqueado != null) {
                        formulario.update { it.copy(ofrecerHuellaParaId = idBloqueado) }
                    }
                }
        }
    }

    fun alCambiarCorreo(valor: String) = formulario.update { it.copy(correo = valor, error = null) }

    fun alCambiarContrasena(valor: String) = formulario.update { it.copy(contrasena = valor, error = null) }

    fun alternarVisibilidad() = formulario.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }

    fun ingresar() {
        val actual = formulario.value
        if (!actual.puedeIngresar) return
        // RF-2: deshabilita «Ingresar» de inmediato, antes de evaluar nada asíncrono (doble pulsación).
        formulario.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            // RF-11: si hay un dueño bloqueado con cambios sin enviar y entra otra persona, se confirma antes.
            when (val decision = evaluarIngresoDeOtraPersona(actual.correo)) {
                is DecisionBorrado.Confirmar ->
                    // Todavía no se autentica, solo se espera confirmación: no deja el formulario colgado.
                    formulario.update { it.copy(cargando = false, confirmarIngreso = decision.pendientes) }
                DecisionBorrado.SinRiesgo -> autenticar()
            }
        }
    }

    private suspend fun autenticar() {
        formulario.update { it.copy(cargando = true, error = null) }
        val actual = formulario.value
        val resultado = iniciarSesion(actual.correo, actual.contrasena)
        formulario.update { it.copy(cargando = false, error = (resultado as? Resultado.Error)?.causa) }
    }

    fun alConfirmarIngreso() {
        formulario.update { it.copy(confirmarIngreso = null) }
        viewModelScope.launch { autenticar() }
    }

    fun alCancelarIngreso() {
        formulario.update { it.copy(confirmarIngreso = null) }
    }

    fun alVerificarHuella(resultado: ResultadoBiometrico) {
        when (resultado) {
            is ResultadoBiometrico.Exito -> desbloquearApp()
            is ResultadoBiometrico.Error -> formulario.update { it.copy(avisoHuella = resultado.mensaje) }
            ResultadoBiometrico.Cancelado -> Unit   // se queda en el acceso: puede reintentar o usar su contraseña
        }
    }

    fun alHuellaOfrecida() = formulario.update { it.copy(ofrecerHuellaParaId = null) }
}