package com.softcorp.sigtec.feature.captura.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.feature.captura.domain.DescartarFoto
import com.softcorp.sigtec.feature.captura.domain.FotoNueva
import com.softcorp.sigtec.feature.captura.domain.GuardarEvidencia
import com.softcorp.sigtec.feature.captura.domain.ObservarCapturaEvidencia
import com.softcorp.sigtec.feature.captura.domain.PrepararFoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class CapturaEvidenciaUiState(
    val cargando: Boolean = true,
    val incidencia: Incidencia? = null,
    val tipo: TipoEvidencia = TipoEvidencia.FALLA,
    val fotos: List<Evidencia> = emptyList(),
    val bloqueo: ErrorDominio? = null,          // RF-929, RF-931: por qué no se pueden agregar más
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val fotoPorTomar: FotoNueva? = null          // evento: la Route la toma con la cámara y avisa
) {
    // RF-927: deshabilitado mientras toma y guarda, para que un doble toque no tome dos fotos
    val puedeDisparar: Boolean
        get() = incidencia != null && bloqueo == null && !guardando
}

/** Lo que pasa con la foto en curso, aparte del Flow de Room. [tipo] es el elegido al disparar. */
private data class AccionCaptura(
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val fotoPorTomar: FotoNueva? = null,
    val tipo: TipoEvidencia = TipoEvidencia.FALLA
)

/** HU-09 (RF-924 a RF-931): «Evidencia fotográfica». Cada foto se guarda al tomarla, no al pulsar «Listo» (P-12). */
@HiltViewModel
class CapturaEvidenciaViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observarCaptura: ObservarCapturaEvidencia,
    private val prepararFoto: PrepararFoto,
    private val guardarEvidencia: GuardarEvidencia,
    private val descartarFoto: DescartarFoto
) : ViewModel() {

    // Ruta.CapturarEvidencia guarda su argumento con el nombre de la propiedad
    private val incidenciaId: String = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])

    // RF-924: empieza en «Falla»; en el SavedStateHandle sobrevive a la rotación
    private val tipo = savedStateHandle.getStateFlow(CLAVE_TIPO, TipoEvidencia.FALLA)

    private val accion = MutableStateFlow(AccionCaptura())

    val estado: StateFlow<CapturaEvidenciaUiState> =
        combine(observarCaptura(incidenciaId), tipo, accion) { captura, tipo, accion ->
            CapturaEvidenciaUiState(
                cargando = false,
                incidencia = captura?.incidencia,
                tipo = tipo,
                fotos = captura?.fotos.orEmpty(),
                bloqueo = captura?.bloqueo,
                guardando = accion.guardando,
                error = accion.error,
                fotoPorTomar = accion.fotoPorTomar
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CapturaEvidenciaUiState())

    fun alCambiarTipo(nuevo: TipoEvidencia) {
        savedStateHandle[CLAVE_TIPO] = nuevo
    }

    /** RF-925: prepara la ruta de la foto y la deja en el estado para que la Route la tome con la cámara. */
    fun alDisparar() {
        if (accion.value.guardando || !estado.value.puedeDisparar) return
        accion.value = AccionCaptura(guardando = true, tipo = tipo.value)
        viewModelScope.launch {
            val foto = prepararFoto(incidenciaId)
            accion.update { it.copy(fotoPorTomar = foto) }
        }
    }

    /** La Route ya le pasó la foto a la cámara: el evento no se repite tras una rotación. */
    fun alFotoEnCamara() = accion.update { it.copy(fotoPorTomar = null) }

    /** La cámara escribió la foto: se enlaza con el tipo elegido al disparar y la hora de ahora. */
    fun alFotoTomada(foto: FotoNueva) {
        viewModelScope.launch {
            val r = guardarEvidencia(incidenciaId, foto, accion.value.tipo, Instant.now())
            accion.value = AccionCaptura(error = (r as? Resultado.Error)?.causa)
        }
    }

    /** RF-928: la cámara falló; se borra lo que haya escrito y se puede volver a disparar. */
    fun alFalloCaptura(foto: FotoNueva) {
        viewModelScope.launch {
            descartarFoto(foto)
            accion.value = AccionCaptura(error = ErrorDominio.Desconocido(null))
        }
    }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        private const val CLAVE_TIPO = "tipo"
    }
}
