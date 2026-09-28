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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun responderOferta(activar: Boolean) {
        viewModelScope.launch {
            if (activar) cambiarIngresoConHuella(true) else rechazarOfertaHuella()
        }
    }
}