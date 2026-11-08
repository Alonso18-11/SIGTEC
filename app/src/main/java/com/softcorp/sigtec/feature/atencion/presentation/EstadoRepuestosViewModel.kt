package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.atencion.domain.ActualizarEstadoRepuestos
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import com.softcorp.sigtec.feature.atencion.domain.SolicitudConIncidencia
import com.softcorp.sigtec.feature.atencion.domain.estadoVisible
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** RF-1121: los filtros de la pantalla. «Pendientes» es todo lo que todavía no se entregó. */
enum class FiltroRepuestos { TODOS, PENDIENTES, ENTREGADOS }

data class EstadoRepuestosUiState(
    val cargando: Boolean = true,
    val filtro: FiltroRepuestos = FiltroRepuestos.TODOS,
    val solicitudes: List<SolicitudConIncidencia> = emptyList(),   // ya filtradas, de la más reciente a la más antigua
    val ultimaConsulta: Instant? = null,                           // RF-1123: la última consulta correcta
    val consultando: Boolean = false,
    val sinRespuesta: Boolean = false,                             // la última consulta no pudo con alguna solicitud
    val ahora: Instant = Instant.now()
)

private data class Consulta(val consultando: Boolean = false, val sinRespuesta: Boolean = false)

/**
 * HU-11 (RF-1121 a RF-1123): las solicitudes del técnico, con un filtro, y la consulta a Logística al abrir la pantalla
 * y al deslizar hacia abajo. Sin conexión muestra lo último guardado. La consulta diaria automática es de la HU-18.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EstadoRepuestosViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActual,
    repo: AtencionRepository,
    private val actualizar: ActualizarEstadoRepuestos
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroRepuestos.TODOS)
    private val consulta = MutableStateFlow(Consulta())

    private val solicitudes = observarUsuarioActual().flatMapLatest { usuario ->
        if (usuario == null) flowOf(emptyList()) else repo.observarSolicitudesDe(usuario.id)
    }

    val estado: StateFlow<EstadoRepuestosUiState> = combine(
        solicitudes, filtro, repo.observarUltimaConsultaLogistica(), consulta
    ) { lista, f, ultima, c ->
        EstadoRepuestosUiState(
            cargando = false,
            filtro = f,
            solicitudes = lista.filter { it.pasaFiltro(f) },
            ultimaConsulta = ultima,
            consultando = c.consultando,
            sinRespuesta = c.sinRespuesta,
            ahora = Instant.now()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoRepuestosUiState())

    init {
        alConsultar()   // RF-1123: al abrir la pantalla
    }

    fun alCambiarFiltro(nuevo: FiltroRepuestos) {
        filtro.value = nuevo
    }

    /** RF-1123: deslizar hacia abajo consulta otra vez; si ya se está consultando, no repite. */
    fun alConsultar() {
        if (consulta.value.consultando) return
        consulta.update { it.copy(consultando = true) }
        viewModelScope.launch {
            val r = actualizar(Instant.now())
            val fallo = r !is Resultado.Exito || r.valor.sinRespuesta > 0
            consulta.value = Consulta(consultando = false, sinRespuesta = fallo)
        }
    }
}

private fun SolicitudConIncidencia.pasaFiltro(filtro: FiltroRepuestos): Boolean {
    val entregada = estadoVisible(solicitud) == EstadoSolicitudVisible.ENTREGADA
    return when (filtro) {
        FiltroRepuestos.TODOS -> true
        FiltroRepuestos.PENDIENTES -> !entregada
        FiltroRepuestos.ENTREGADOS -> entregada
    }
}
