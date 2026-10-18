package com.softcorp.sigtec.feature.captura.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.feature.captura.domain.LecturaCodigo
import com.softcorp.sigtec.feature.captura.domain.LeerCodigoEquipo
import com.softcorp.sigtec.feature.captura.domain.RachaLectura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EscaneoUiState(
    val racha: RachaLectura = RachaLectura(),
    val reconocido: LecturaCodigo? = null       // RF-906: con un código, la pantalla deja de analizar
)

/** HU-09: «Escanear etiqueta». Recibe el texto de cada imagen y decide cuándo hay un código reconocido. */
@HiltViewModel
class EscaneoViewModel @Inject constructor(
    private val leerCodigo: LeerCodigoEquipo
) : ViewModel() {

    private val _estado = MutableStateFlow(EscaneoUiState())
    val estado: StateFlow<EscaneoUiState> = _estado.asStateFlow()

    // El analizador llama desde su hilo: se procesa un texto a la vez y, si llegan más, gana el último (P-9)
    private val textos = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch { textos.collect { procesar(it) } }
    }

    fun alLeerTexto(texto: String) {
        if (_estado.value.reconocido == null) textos.tryEmit(texto)
    }

    /** RF-908: se descarta el código mostrado y la racha empieza de nuevo. */
    fun alVolverAEscanear() {
        _estado.value = EscaneoUiState()
    }

    private suspend fun procesar(texto: String) {
        if (_estado.value.reconocido != null) return
        val lectura = leerCodigo(texto)
        // RF-905: solo con 3 lecturas iguales seguidas
        val racha = _estado.value.racha.siguiente(lectura?.codigo)
        _estado.value = EscaneoUiState(racha, reconocido = lectura.takeIf { racha.estable })
    }
}
