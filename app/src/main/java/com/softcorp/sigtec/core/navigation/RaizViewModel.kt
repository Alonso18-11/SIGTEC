package com.softcorp.sigtec.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.core.domain.usecase.CambiarIngresoConHuella
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoAcceso
import com.softcorp.sigtec.core.domain.usecase.ObservarPreferenciasHuella
import com.softcorp.sigtec.core.domain.usecase.RechazarOfertaHuella
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RaizViewModel @Inject constructor(
    observarEstadoAcceso: ObservarEstadoAcceso,
    observarPreferenciasHuella: ObservarPreferenciasHuella,
    private val cambiarIngresoConHuella: CambiarIngresoConHuella,
    private val rechazarOfertaHuella: RechazarOfertaHuella,
    evaluador: EvaluadorBiometrico
) : ViewModel() {

    val disponibilidad: DisponibilidadBiometrica = evaluador.evaluar()

    val estadoAcceso: StateFlow<EstadoAcceso> = observarEstadoAcceso()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoAcceso.SinSesion)

    // Se ofrece una sola vez y solo si el celular aún no tiene dueño de huella:
    // quien entra en el celular de otra persona nunca ve esta oferta.
    val mostrarOfertaHuella: StateFlow<Boolean> =
        combine(estadoAcceso, observarPreferenciasHuella()) { estado, prefs ->
            estado is EstadoAcceso.Activa &&
                    !prefs.activada &&
                    !prefs.ofrecida &&
                    disponibilidad.permiteIngreso
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // HU-07 (RF-711 a RF-713, D-7): el aviso tocado espera aquí hasta que la sesión esté activa.
    // Vive en el ViewModel para sobrevivir a la rotación y al arranque en frío, que empieza sin sesión.
    private val _avisoPendiente = MutableStateFlow<AvisoTocado?>(null)
    val avisoPendiente: StateFlow<AvisoTocado?> = _avisoPendiente.asStateFlow()

    fun alTocarAviso(aviso: AvisoTocado) {
        _avisoPendiente.value = aviso
    }

    fun alAtenderAviso() {
        _avisoPendiente.value = null
    }

    fun responderOferta(activar: Boolean) {
        viewModelScope.launch {
            if (activar) cambiarIngresoConHuella(true) else rechazarOfertaHuella()
        }
    }
}