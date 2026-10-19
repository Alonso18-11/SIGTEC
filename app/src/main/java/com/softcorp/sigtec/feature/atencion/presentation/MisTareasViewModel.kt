package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.feature.atencion.domain.ObservarBandeja
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class MisTareasUiState(
    val cargando: Boolean = true,
    val incidencias: List<Incidencia> = emptyList(),
    val ahora: Instant = Instant.now()
)

@HiltViewModel
class MisTareasViewModel @Inject constructor(
    observarBandeja: ObservarBandeja
) : ViewModel() {
    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    val estado: StateFlow<MisTareasUiState> =
        combine(observarBandeja(), reloj) { bandeja, ahora ->
            MisTareasUiState(cargando = false, incidencias = bandeja, ahora = ahora)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisTareasUiState())

    private companion object {
        const val RELOJ_MS = 60_000L
    }
}
