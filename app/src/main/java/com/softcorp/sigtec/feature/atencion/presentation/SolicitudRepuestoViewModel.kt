package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import com.softcorp.sigtec.feature.atencion.domain.Disponibilidad
import com.softcorp.sigtec.feature.atencion.domain.LogisticaRepository
import com.softcorp.sigtec.feature.atencion.domain.PiezaConsultada
import com.softcorp.sigtec.feature.atencion.domain.PiezaLogistica
import com.softcorp.sigtec.feature.atencion.domain.ResultadoLogistica
import com.softcorp.sigtec.feature.atencion.domain.SolicitarRepuesto
import com.softcorp.sigtec.feature.atencion.domain.disponibilidadDe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SolicitudRepuestoUiState(
    val incidencia: Incidencia? = null,
    val pieza: String = "",
    val motivo: String = "",
    val coincidencias: List<PiezaLogistica> = emptyList(),   // RF-1104: hasta 5 para elegir
    val elegida: PiezaLogistica? = null,
    val disponibilidad: Disponibilidad? = null,               // null = nada que mostrar todavía
    val enviando: Boolean = false,
    val error: ErrorDominio? = null,
    val disponibilidadBiometrica: DisponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD,
    // Evento de un solo uso: la solicitud quedó guardada y se vuelve al detalle (RF-1114)
    val registradaId: String? = null
) {
    // RF-1110: pieza y motivo, sin otro envío en curso y con huella o bloqueo de pantalla en el celular
    val puedeEnviar: Boolean
        get() = pieza.isNotBlank() && motivo.isNotBlank() && !enviando && disponibilidadBiometrica.puedeConfirmar
}

/**
 * HU-11 (plan 4.2): busca la pieza en Logística mientras se escribe y envía la solicitud tras la confirmación
 * biométrica. Consultar Logística nunca bloquea el envío: sin respuesta se puede pedir igual (D-2).
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SolicitudRepuestoViewModel : ViewModel {

    private val incidenciaId: String
    private val solicitarRepuesto: SolicitarRepuesto
    private val logistica: LogisticaRepository
    private val _estado: MutableStateFlow<SolicitudRepuestoUiState>
    val estado: StateFlow<SolicitudRepuestoUiState>

    // RF-1104: lo que escribe el técnico; se consulta cuando deja de escribir
    private val busqueda = MutableStateFlow("")

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        solicitarRepuesto: SolicitarRepuesto,
        atencion: AtencionRepository,
        logistica: LogisticaRepository,
        evaluador: EvaluadorBiometrico
    ) : this(savedStateHandle, solicitarRepuesto, atencion, logistica, evaluador.evaluar())

    constructor(
        savedStateHandle: SavedStateHandle,
        solicitarRepuesto: SolicitarRepuesto,
        atencion: AtencionRepository,
        logistica: LogisticaRepository,
        disponibilidadBiometrica: DisponibilidadBiometrica
    ) : super() {
        incidenciaId = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])
        this.solicitarRepuesto = solicitarRepuesto
        this.logistica = logistica
        _estado = MutableStateFlow(SolicitudRepuestoUiState(disponibilidadBiometrica = disponibilidadBiometrica))
        estado = _estado.asStateFlow()

        viewModelScope.launch {
            val incidencia = atencion.incidencia(incidenciaId)
            _estado.update { it.copy(incidencia = incidencia) }
        }
        // RF-1106: collectLatest cancela la consulta anterior si llega texto nuevo; solo cuenta la última
        viewModelScope.launch {
            busqueda.drop(1).debounce(ESPERA_MS).collectLatest { texto ->
                if (texto.trim().length >= MINIMO_LETRAS) buscar(texto)
            }
        }
    }

    /** RF-1109: editar la pieza quita lo que se mostraba de la anterior y vuelve a consultar. */
    fun alCambiarPieza(valor: String) {
        _estado.update {
            it.copy(pieza = valor, elegida = null, coincidencias = emptyList(), disponibilidad = null, error = null)
        }
        busqueda.value = valor
    }

    fun alCambiarMotivo(valor: String) = _estado.update { it.copy(motivo = valor, error = null) }

    /** RF-1105: al elegir una pieza se escribe su nombre y se ve si hay en almacén. */
    fun alElegirPieza(pieza: PiezaLogistica) = _estado.update {
        it.copy(
            pieza = pieza.nombre,
            elegida = pieza,
            coincidencias = emptyList(),
            disponibilidad = disponibilidadDe(pieza.stock, pieza.diasEnvio)
        )
    }

    /** RF-1108: «Reintentar» consulta de inmediato, sin esperar a que se escriba otra vez. */
    fun alReintentar() {
        val texto = _estado.value.pieza
        if (texto.trim().length >= MINIMO_LETRAS) viewModelScope.launch { buscar(texto) }
    }

    /** RF-1113: con la verificación biométrica correcta, [confirmadoEn] es su instante. */
    fun alConfirmar(confirmadoEn: Instant) {
        val actual = _estado.value
        if (!actual.puedeEnviar) return   // RF-1116: un segundo toque no registra otra solicitud
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val consultada = actual.elegida?.let { PiezaConsultada(it.stock, it.diasEnvio) }
            when (val r = solicitarRepuesto(incidenciaId, actual.pieza, actual.motivo, consultada, confirmadoEn, Instant.now())) {
                // «enviando» no vuelve a false: la pantalla se cierra al volver al detalle
                is Resultado.Exito -> _estado.update { it.copy(registradaId = r.valor.id) }
                is Resultado.Error -> _estado.update { it.copy(enviando = false, error = r.causa) }   // RF-1115
            }
        }
    }

    fun alVolverAlDetalle() = _estado.update { it.copy(registradaId = null) }

    private suspend fun buscar(texto: String) {
        _estado.update { it.copy(disponibilidad = Disponibilidad.Consultando, coincidencias = emptyList()) }   // RF-1106
        val respuesta = logistica.buscarPiezas(texto)
        // Si mientras tanto eligió una pieza o cambió el texto, esta respuesta ya no aplica
        if (_estado.value.pieza != texto || _estado.value.elegida != null) return
        _estado.update {
            when (respuesta) {
                is ResultadoLogistica.Exito ->
                    if (respuesta.valor.isEmpty()) it.copy(disponibilidad = Disponibilidad.NoEnCatalogo)   // RF-1107
                    else it.copy(disponibilidad = null, coincidencias = respuesta.valor.take(MAX_COINCIDENCIAS))
                is ResultadoLogistica.Fallo -> it.copy(disponibilidad = Disponibilidad.SinRespuesta)          // RF-1108
            }
        }
    }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        const val ESPERA_MS = 600L          // RF-1104
        const val MINIMO_LETRAS = 3
        const val MAX_COINCIDENCIAS = 5
    }
}
