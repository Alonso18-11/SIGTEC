package com.softcorp.sigtec.feature.captura.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado
import com.softcorp.sigtec.feature.captura.domain.EventoDictado
import com.softcorp.sigtec.feature.captura.domain.ReconocedorVoz
import com.softcorp.sigtec.feature.captura.domain.unirTexto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DictadoUiState(
    val campo: CampoDictado = CampoDictado.DESCRIPCION,
    val subtitulo: String = "",
    val escuchando: Boolean = false,
    val texto: String = "",          // lo ya confirmado por las escuchas anteriores (RF-1008)
    val parcial: String = "",        // lo que se va reconociendo en la escucha en curso (RF-1005)
    val error: ErrorDictado? = null,
    val sinServicio: Boolean = false, // RF-1017
    // Evento de un solo uso: el texto que hay que entregar al campo (RF-1010)
    val textoListo: String? = null
) {
    val textoVisible: String get() = unirTexto(texto, parcial)

    val puedeUsar: Boolean get() = textoVisible.isNotBlank()   // RF-1009
}

/**
 * HU-10 (plan 4.3): una escucha por toque (P-2). El texto se conserva al detenerse, ante un error y al dejar de
 * ver la pantalla; solo se entrega al pulsar «Usar este texto». No llama a `VerificarPermiso`: dictar no cambia
 * datos (P-7).
 */
@HiltViewModel
class DictadoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reconocedor: ReconocedorVoz
) : ViewModel() {

    private val _estado = MutableStateFlow(
        DictadoUiState(
            campo = CampoDictado.valueOf(checkNotNull(savedStateHandle[ARG_CAMPO])),
            subtitulo = savedStateHandle[ARG_SUBTITULO] ?: "",
            sinServicio = !reconocedor.disponible
        )
    )
    val estado: StateFlow<DictadoUiState> = _estado.asStateFlow()

    private var escucha: Job? = null
    private var empezoUnaVez = false

    /** RF-1004 y RF-1020: con el permiso concedido empieza solo, pero solo la primera vez (RF-1014). */
    fun alPermisoConcedido() {
        if (empezoUnaVez) return
        empezoUnaVez = true
        escuchar()
    }

    /** RF-1008: vuelve a escuchar y suma lo nuevo al final. */
    fun alSeguirDictando() = escuchar()

    /** RF-1007: Android entrega lo reconocido y la escucha termina sola. */
    fun alDetener() = reconocedor.detener()

    /** RF-1014: la pantalla dejó de estar a la vista; se suelta el micrófono y se conserva el texto. */
    fun alDejarDeVer() {
        escucha?.cancel()
        escucha = null
        _estado.update { it.copy(texto = it.textoVisible, parcial = "", escuchando = false) }
    }

    /** RF-1010: entrega el texto, sin espacios en los extremos. */
    fun alUsarTexto() {
        alDejarDeVer()
        _estado.update { it.copy(textoListo = it.texto.trim()) }
    }

    fun alTextoEntregado() = _estado.update { it.copy(textoListo = null) }

    private fun escuchar() {
        if (escucha?.isActive == true || _estado.value.sinServicio) return
        _estado.update { it.copy(error = null) }
        escucha = viewModelScope.launch {
            reconocedor.escuchar().collect { evento -> aplicar(evento) }
        }
    }

    private fun aplicar(evento: EventoDictado) = _estado.update { actual ->
        when (evento) {
            EventoDictado.Escuchando -> actual.copy(escuchando = true)
            is EventoDictado.Parcial -> actual.copy(parcial = evento.texto)
            is EventoDictado.Final ->
                actual.copy(texto = unirTexto(actual.texto, evento.texto), parcial = "", escuchando = false)
            // RF-1015 a RF-1018: el texto que ya había no se pierde
            is EventoDictado.Error -> actual.copy(
                texto = actual.textoVisible,
                parcial = "",
                escuchando = false,
                error = evento.error,
                sinServicio = actual.sinServicio || evento.error == ErrorDictado.SIN_SERVICIO
            )
        }
    }

    companion object {
        // Los mismos nombres que los argumentos de Ruta.DictarTexto
        const val ARG_CAMPO = "campo"
        const val ARG_SUBTITULO = "subtitulo"
    }
}
