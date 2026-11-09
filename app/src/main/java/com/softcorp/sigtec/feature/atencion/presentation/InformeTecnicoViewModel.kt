package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.feature.atencion.domain.CerrarIncidenciaConInforme
import com.softcorp.sigtec.feature.captura.domain.ObservarCapturaEvidencia
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.unirTexto
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
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
    val cerrada: Boolean = false,
    val evidencias: List<Evidencia> = emptyList(),   // HU-09 (RF-938): las fotos de la incidencia en este celular
    val puedeAgregarFoto: Boolean = false,           // HU-09 (RF-923): el técnico asignado, mientras siga pendiente
    val codigoIncidencia: String? = null             // HU-10 (RF-1003): para el subtítulo de «Dictar»
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
        observarCaptura: ObservarCapturaEvidencia,
        evaluador: EvaluadorBiometrico
    ) : this(savedStateHandle, cerrarIncidenciaConInforme, observarCaptura, evaluador.evaluar())

    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        observarCaptura: ObservarCapturaEvidencia,
        disponibilidadBiometrica: DisponibilidadBiometrica
    ) : super() {
        incidenciaId = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])
        this.cerrarIncidenciaConInforme = cerrarIncidenciaConInforme
        _estado = MutableStateFlow(InformeTecnicoUiState(disponibilidadBiometrica = disponibilidadBiometrica))
        estado = _estado.asStateFlow()
        // RF-938: las fotos entran al mismo estado del formulario, así que nunca pisan lo escrito. Con el máximo de
        // fotos el botón sigue, igual que en el detalle: «Evidencia fotográfica» avisa del límite (RF-929).
        viewModelScope.launch {
            observarCaptura(incidenciaId).collect { captura ->
                _estado.update {
                    it.copy(
                        evidencias = captura?.fotos.orEmpty(),
                        codigoIncidencia = captura?.incidencia?.let { inc -> inc.codigoVisible ?: POR_SINCRONIZAR },
                        puedeAgregarFoto = captura != null &&
                                (captura.bloqueo == null || captura.bloqueo == ErrorDominio.LimiteFotosAlcanzado)
                    )
                }
            }
        }
    }

    fun alCambiarDiagnostico(valor: String) = _estado.update { it.copy(diagnostico = valor, error = null) }

    fun alCambiarTrabajo(valor: String) = _estado.update { it.copy(trabajoRealizado = valor, error = null) }

    fun alCambiarPiezas(valor: String) = _estado.update { it.copy(piezasCambiadas = valor, error = null) }

    fun alCambiarObservaciones(valor: String) = _estado.update { it.copy(observaciones = valor, error = null) }

    /**
     * HU-10 (RF-1011): lo dictado se agrega al final de su campo. Con el informe guardándose o ya confirmado, el
     * formulario no es editable y no cambia nada (RF-1002).
     */
    fun alRecibirDictado(campo: CampoDictado, texto: String) = _estado.update {
        if (it.guardando || it.cerrada) return@update it
        when (campo) {
            CampoDictado.DIAGNOSTICO -> it.copy(diagnostico = unirTexto(it.diagnostico, texto), error = null)
            CampoDictado.SOLUCION -> it.copy(trabajoRealizado = unirTexto(it.trabajoRealizado, texto), error = null)
            CampoDictado.OBSERVACIONES -> it.copy(observaciones = unirTexto(it.observaciones, texto), error = null)
            CampoDictado.DESCRIPCION, CampoDictado.SINTOMAS -> it   // no son campos del informe
        }
    }

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
