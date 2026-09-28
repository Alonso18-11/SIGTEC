package com.softcorp.sigtec.feature.acceso.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.usecase.BloquearApp
import com.softcorp.sigtec.core.domain.usecase.CambiarIngresoConHuella
import com.softcorp.sigtec.core.domain.usecase.CerrarSesion
import com.softcorp.sigtec.core.domain.usecase.ObservarPreferenciasHuella
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AjustesUiState(
    val usuario: Usuario? = null,
    val preferencias: PreferenciasHuella = PreferenciasHuella(),
    val disponibilidad: DisponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD
) {
    val esPropietario: Boolean
        get() = usuario != null && preferencias.esPropietario(usuario.id)

    /** La huella del celular pertenece a otra persona: este usuario no puede tocarla. */
    val huellaDeOtraPersona: Boolean
        get() = preferencias.activada && !esPropietario

    val puedeActivar: Boolean
        get() = !preferencias.activada && disponibilidad.permiteIngreso
}

@HiltViewModel
class AjustesSeguridadViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActual,
    observarPreferenciasHuella: ObservarPreferenciasHuella,
    private val cambiarIngresoConHuella: CambiarIngresoConHuella,
    private val bloquearApp: BloquearApp,
    private val cerrarSesion: CerrarSesion,
    private val evaluador: EvaluadorBiometrico
) : ViewModel() {

    private val disponibilidad = MutableStateFlow(evaluador.evaluar())

    val estado: StateFlow<AjustesUiState> = combine(
        observarUsuarioActual(), observarPreferenciasHuella(), disponibilidad
    ) { usuario, prefs, disp ->
        AjustesUiState(usuario, prefs, disp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState())

    /** Al volver de los ajustes de Android, el usuario pudo haber registrado una huella. */
    fun reevaluar() { disponibilidad.value = evaluador.evaluar() }

    // La pantalla solo ofrece lo permitido; si aun así llegara un cambio ajeno,
    // el caso de uso lo rechaza con SinPermiso.
    fun cambiarHuella(activar: Boolean) {
        viewModelScope.launch { cambiarIngresoConHuella(activar) }
    }

    fun bloquear() = bloquearApp()

    fun salir() {
        viewModelScope.launch { cerrarSesion() }
    }
}