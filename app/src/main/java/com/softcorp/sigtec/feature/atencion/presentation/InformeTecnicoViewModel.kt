package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.atencion.domain.CerrarIncidenciaConInforme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class InformeTecnicoUiState(
    val diagnostico: String = "",
    val trabajoRealizado: String = "",
    val piezasCambiadas: String = "",
    val observaciones: String = "",
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val disponibilidadBiometrica: DisponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD,
    val cerrada: Boolean = false
) {
    val puedeConfirmar: Boolean
        get() = diagnostico.isNotBlank() && trabajoRealizado.isNotBlank() && !guardando &&
                disponibilidadBiometrica.puedeConfirmar
}

@HiltViewModel
class InformeTecnicoViewModel : ViewModel {

    private val incidenciaId: String
    private val cerrarIncidenciaConInforme: CerrarIncidenciaConInforme
    private val _estado: MutableStateFlow<InformeTecnicoUiState>
    val estado: StateFlow<InformeTecnicoUiState>

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        evaluador: EvaluadorBiometrico
    ) : this(savedStateHandle, cerrarIncidenciaConInforme, evaluador.evaluar())

    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        disponibilidadBiometrica: DisponibilidadBiometrica
    ) : super() {
        incidenciaId = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])
        this.cerrarIncidenciaConInforme = cerrarIncidenciaConInforme
        _estado = MutableStateFlow(InformeTecnicoUiState(disponibilidadBiometrica = disponibilidadBiometrica))
        estado = _estado.asStateFlow()
    }

    fun alCambiarDiagnostico(valor: String) = _estado.update { it.copy(diagnostico = valor, error = null) }

    fun alCambiarTrabajo(valor: String) = _estado.update { it.copy(trabajoRealizado = valor, error = null) }

    fun alCambiarPiezas(valor: String) = _estado.update { it.copy(piezasCambiadas = valor, error = null) }

    fun alCambiarObservaciones(valor: String) = _estado.update { it.copy(observaciones = valor, error = null) }

    fun alConfirmarCierre(confirmadoEn: Instant) {
        val actual = _estado.value
        if (!actual.puedeConfirmar) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (
                val r = cerrarIncidenciaConInforme(
                    incidenciaId = incidenciaId,
                    diagnostico = actual.diagnostico,
                    trabajoRealizado = actual.trabajoRealizado,
                    piezasCambiadas = actual.piezasCambiadas,
                    observaciones = actual.observaciones,
                    confirmadoEn = confirmadoEn
                )
            ) {

                is Resultado.Exito -> _estado.update { it.copy(cerrada = true) }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    private companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
    }
}
