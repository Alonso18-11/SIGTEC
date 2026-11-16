package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.atencion.domain.CerrarIncidenciaConInforme
import com.softcorp.sigtec.feature.captura.domain.ObservarCapturaEvidencia
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.unirTexto
import com.softcorp.sigtec.feature.ia.domain.BorradorInforme
import com.softcorp.sigtec.feature.ia.domain.DecidirSugerencia
import com.softcorp.sigtec.feature.ia.domain.EntradaBorrador
import com.softcorp.sigtec.feature.ia.domain.ObservarIADisponible
import com.softcorp.sigtec.feature.ia.domain.ObservarSugerencia
import com.softcorp.sigtec.feature.ia.domain.RedactarBorradorInforme
import com.softcorp.sigtec.feature.ia.domain.leerBorrador
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
    val codigoIncidencia: String? = null,            // HU-10 (RF-1003): para el subtítulo de «Dictar»
    val notas: String = "",                          // HU-14 (RF-1414): solo para el borrador; no se guardan
    // HU-14 (RF-1415 a RF-1424): el borrador sin decidir, si se está redactando y si lo último salió de la IA
    val iaDisponible: Boolean = false,
    val redactando: Boolean = false,
    val borrador: SugerenciaIA? = null,
    val errorBorrador: ErrorDominio? = null,
    val textoDeIA: Boolean = false
) {
    val borradorLeido: BorradorInforme? get() = borrador?.let { leerBorrador(it.contenido) }

    // RF-1415: con notas, con conexión y sin otro pedido ni el cierre en curso
    val puedeRedactar: Boolean
        get() = notas.isNotBlank() && iaDisponible && !redactando && !guardando && !cerrada

    val puedeConfirmar: Boolean
        get() = diagnostico.isNotBlank() && trabajoRealizado.isNotBlank() && !guardando &&
                disponibilidadBiometrica.puedeConfirmar
}

@HiltViewModel
class InformeTecnicoViewModel : ViewModel {

    private val incidenciaId: String
    private val savedStateHandle: SavedStateHandle
    private val cerrarIncidenciaConInforme: CerrarIncidenciaConInforme
    private val redactarBorradorInforme: RedactarBorradorInforme
    private val decidirSugerencia: DecidirSugerencia
    private var incidencia: Incidencia? = null
    private val _estado: MutableStateFlow<InformeTecnicoUiState>
    val estado: StateFlow<InformeTecnicoUiState>

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        observarCaptura: ObservarCapturaEvidencia,
        evaluador: EvaluadorBiometrico,
        redactarBorradorInforme: RedactarBorradorInforme,
        observarSugerencia: ObservarSugerencia,
        observarIADisponible: ObservarIADisponible,
        decidirSugerencia: DecidirSugerencia
    ) : this(
        savedStateHandle, cerrarIncidenciaConInforme, observarCaptura, evaluador.evaluar(),
        redactarBorradorInforme, observarSugerencia, observarIADisponible, decidirSugerencia
    )

    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        observarCaptura: ObservarCapturaEvidencia,
        disponibilidadBiometrica: DisponibilidadBiometrica,
        redactarBorradorInforme: RedactarBorradorInforme,
        observarSugerencia: ObservarSugerencia,
        observarIADisponible: ObservarIADisponible,
        decidirSugerencia: DecidirSugerencia
    ) : super() {
        incidenciaId = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])
        this.savedStateHandle = savedStateHandle
        this.cerrarIncidenciaConInforme = cerrarIncidenciaConInforme
        this.redactarBorradorInforme = redactarBorradorInforme
        this.decidirSugerencia = decidirSugerencia
        // RF-1420: el aviso de texto redactado por IA sobrevive a una rotación
        _estado = MutableStateFlow(
            InformeTecnicoUiState(
                disponibilidadBiometrica = disponibilidadBiometrica,
                textoDeIA = savedStateHandle[CLAVE_TEXTO_IA] ?: false
            )
        )
        estado = _estado.asStateFlow()
        // RF-938: las fotos entran al mismo estado del formulario, así que nunca pisan lo escrito. Con el máximo de
        // fotos el botón sigue, igual que en el detalle: «Evidencia fotográfica» avisa del límite (RF-929).
        viewModelScope.launch {
            observarCaptura(incidenciaId).collect { captura ->
                incidencia = captura?.incidencia
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
        // HU-14 (RF-1424): el borrador sin decidir se ve al volver, sin pedir otro
        viewModelScope.launch {
            observarSugerencia(incidenciaId, TipoSugerencia.BORRADOR_INFORME).collect { borrador ->
                _estado.update { it.copy(borrador = borrador) }
            }
        }
        viewModelScope.launch {
            observarIADisponible().collect { disponible -> _estado.update { it.copy(iaDisponible = disponible) } }
        }
    }

    fun alCambiarDiagnostico(valor: String) = _estado.update { it.copy(diagnostico = valor, error = null) }

    fun alCambiarTrabajo(valor: String) = _estado.update { it.copy(trabajoRealizado = valor, error = null) }

    fun alCambiarPiezas(valor: String) = _estado.update { it.copy(piezasCambiadas = valor, error = null) }

    fun alCambiarObservaciones(valor: String) = _estado.update { it.copy(observaciones = valor, error = null) }

    fun alCambiarNotas(valor: String) = _estado.update { it.copy(notas = valor) }

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
            CampoDictado.NOTAS -> it.copy(notas = unirTexto(it.notas, texto))                // HU-14 (RF-1414)
            CampoDictado.DESCRIPCION, CampoDictado.SINTOMAS -> it   // no son campos del informe
        }
    }

    /** HU-14 (RF-1416 a RF-1419, RF-1423): el informe no cambia hasta que el técnico use el borrador. */
    fun alRedactarBorrador() {
        val actual = _estado.value
        val incidencia = incidencia ?: return
        if (!actual.puedeRedactar) return
        _estado.update { it.copy(redactando = true, errorBorrador = null) }
        viewModelScope.launch {
            val entrada = EntradaBorrador(
                descripcion = incidencia.descripcion, notas = actual.notas, diagnostico = actual.diagnostico,
                trabajoRealizado = actual.trabajoRealizado, piezasCambiadas = actual.piezasCambiadas,
                observaciones = actual.observaciones
            )
            val error = (redactarBorradorInforme(incidencia, entrada, Instant.now()) as? Resultado.Error)?.causa
            _estado.update { it.copy(redactando = false, errorBorrador = error?.let(::errorDelBorrador)) }
        }
    }

    /** RF-1420: cada campo que el borrador trae reemplaza al del informe; los vacíos no tocan nada. */
    fun alUsarBorrador() {
        val actual = _estado.value
        val sugerencia = actual.borrador ?: return
        val borrador = actual.borradorLeido ?: return
        _estado.update {
            it.copy(
                diagnostico = borrador.diagnostico.ifBlank { it.diagnostico },
                trabajoRealizado = borrador.trabajoRealizado.ifBlank { it.trabajoRealizado },
                piezasCambiadas = borrador.piezasCambiadas.ifBlank { it.piezasCambiadas },
                observaciones = borrador.observaciones.ifBlank { it.observaciones },
                textoDeIA = true,
                error = null
            )
        }
        savedStateHandle[CLAVE_TEXTO_IA] = true
        viewModelScope.launch { decidirSugerencia(sugerencia, aceptada = true, ahora = Instant.now()) }
    }

    /** RF-1421: no cambia los campos. */
    fun alDescartarBorrador() {
        val sugerencia = _estado.value.borrador ?: return
        viewModelScope.launch { decidirSugerencia(sugerencia, aceptada = false, ahora = Instant.now()) }
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

                is Resultado.Exito -> {
                    // HU-14 (RF-1426): un borrador sin decidir queda como no aceptado
                    actual.borrador?.let { decidirSugerencia(it, aceptada = false, ahora = Instant.now()) }
                    _estado.update { it.copy(cerrada = true) }
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    /** RF-1423: sin conexión o con la IA caída se ve lo mismo; otros errores (permiso, notas) con su texto. */
    private fun errorDelBorrador(error: ErrorDominio): ErrorDominio =
        if (error == ErrorDominio.SinConexion || error is ErrorDominio.Desconocido) ErrorDominio.IANoDisponible else error

    private companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        const val CLAVE_TEXTO_IA = "textoDeIA"
    }
}
