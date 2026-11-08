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
import kotlinx.coroutines.delay
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
 * HU-10 (plan 4.3): escucha hasta que la persona pulsa «Detener» (D-10). Android corta cada tramo al detectar una
 * pausa; aquí se suma lo reconocido y se vuelve a escuchar solo, hasta «Detener» o hasta [TOPE_SILENCIO_MS] sin
 * palabras nuevas. El texto se conserva al detenerse, ante un error y al dejar de ver la pantalla; solo se entrega
 * al pulsar «Usar este texto». No llama a `VerificarPermiso`: dictar no cambia datos (P-7).
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
    private var tope: Job? = null
    private var empezoUnaVez = false

    // Falso desde que la persona pulsa «Detener», sale de la pantalla o se cumple el tope
    private var seguirEscuchando = false

    /** RF-1004 y RF-1020: con el permiso concedido empieza solo, pero solo la primera vez (RF-1014). */
    fun alPermisoConcedido() {
        if (empezoUnaVez) return
        empezoUnaVez = true
        escuchar()
    }

    /** RF-1008: vuelve a escuchar y suma lo nuevo al final. */
    fun alSeguirDictando() = escuchar()

    /** RF-1007: Android entrega lo reconocido y ya no se vuelve a escuchar. */
    fun alDetener() {
        seguirEscuchando = false
        tope?.cancel()
        reconocedor.detener()
    }

    /** RF-1014: la pantalla dejó de estar a la vista; se suelta el micrófono y se conserva el texto. */
    fun alDejarDeVer() {
        seguirEscuchando = false
        tope?.cancel()
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
        seguirEscuchando = true
        _estado.update { it.copy(error = null, escuchando = true) }
        reiniciarTope()
        escucha = viewModelScope.launch {
            // D-10: cada tramo que Android cierra por una pausa se suma y se vuelve a escuchar
            while (true) {
                var ultimo: EventoDictado? = null
                reconocedor.escuchar().collect { evento ->
                    ultimo = evento
                    aplicar(evento)
                }
                val siguePorPausa = ultimo is EventoDictado.Final ||
                        (ultimo as? EventoDictado.Error)?.error == ErrorDictado.SIN_ENTENDER
                if (!seguirEscuchando || !siguePorPausa) break
                delay(REINICIO_MS)
                if (!seguirEscuchando) break
            }
            // RF-1015: «Detener» sin que se haya entendido nada sí se avisa
            val sinTexto = _estado.value.textoVisible.isBlank()
            _estado.update { it.copy(escuchando = false, error = it.error ?: errorSiNoSeEntendio(sinTexto)) }
            tope?.cancel()
        }
    }

    // Se recuerda si el último tramo fue silencio, para avisar «No se entendió» solo si al final no hay texto
    private var ultimoFueSilencio = false

    private fun errorSiNoSeEntendio(sinTexto: Boolean): ErrorDictado? =
        if (ultimoFueSilencio && sinTexto) ErrorDictado.SIN_ENTENDER else null

    /** D-10: si pasan [TOPE_SILENCIO_MS] sin palabras nuevas, se detiene solo para no dejar el micrófono encendido. */
    private fun reiniciarTope() {
        tope?.cancel()
        tope = viewModelScope.launch {
            delay(TOPE_SILENCIO_MS)
            alDetener()
        }
    }

    private fun aplicar(evento: EventoDictado) {
        ultimoFueSilencio = (evento as? EventoDictado.Error)?.error == ErrorDictado.SIN_ENTENDER
        val hayPalabras = (evento as? EventoDictado.Parcial)?.texto?.isNotBlank() == true ||
                (evento as? EventoDictado.Final)?.texto?.isNotBlank() == true
        if (hayPalabras && seguirEscuchando) reiniciarTope()
        _estado.update { actual ->
            when (evento) {
                EventoDictado.Escuchando -> actual.copy(escuchando = true)
                is EventoDictado.Parcial -> actual.copy(parcial = evento.texto)
                // Al detener, algunos servicios de voz entregan el final vacío: entonces vale lo que ya se veía
                is EventoDictado.Final -> actual.copy(
                    texto = unirTexto(actual.texto, evento.texto.ifBlank { actual.parcial }),
                    parcial = ""
                )
                // Un silencio mientras se sigue escuchando no es un error: solo se guarda lo que se veía (D-10)
                is EventoDictado.Error -> if (seguirEscuchando && evento.error == ErrorDictado.SIN_ENTENDER) {
                    actual.copy(texto = actual.textoVisible, parcial = "")
                } else {
                    // RF-1015 a RF-1018: el texto que ya había no se pierde
                    actual.copy(
                        texto = actual.textoVisible,
                        parcial = "",
                        error = evento.error.takeUnless { it == ErrorDictado.SIN_ENTENDER },
                        sinServicio = actual.sinServicio || evento.error == ErrorDictado.SIN_SERVICIO
                    )
                }
            }
        }
        if (evento is EventoDictado.Error && evento.error != ErrorDictado.SIN_ENTENDER) seguirEscuchando = false
    }

    companion object {
        // Los mismos nombres que los argumentos de Ruta.DictarTexto
        const val ARG_CAMPO = "campo"
        const val ARG_SUBTITULO = "subtitulo"

        // D-10: sin palabras nuevas durante este tiempo, se detiene solo
        const val TOPE_SILENCIO_MS = 60_000L

        // Pausa corta antes de volver a escuchar, para no reabrir el micrófono en un bucle si el servicio corta enseguida
        const val REINICIO_MS = 250L
    }
}
