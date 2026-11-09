package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.indicadores.domain.Indicadores
import com.softcorp.sigtec.feature.indicadores.domain.ObservarIndicadores
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class IndicadoresUiState(
    val cargando: Boolean = true,
    val sinPermiso: Boolean = false,      // RF-1503
    val nombre: String = "",
    val cargo: String = "",
    val actualizado: String = "",         // RF-1502
    val indicadores: Indicadores? = null
)

/** HU-15 (RF-1501 a RF-1504): el tablero del jefe, calculado con Room y recalculado cada minuto. */
@HiltViewModel
class IndicadoresViewModel @Inject constructor(
    observarIndicadores: ObservarIndicadores,
    observarUsuarioActual: ObservarUsuarioActual,
    observarEstadoDatos: ObservarEstadoDatos
) : ViewModel() {

    // «Hoy» y los 7 días no deben quedar desactualizados si la pantalla queda abierta (mismo patrón que la lista)
    private val reloj: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val resultado = observarIndicadores(reloj)
    private val sinPermiso = resultado is Resultado.Error
    private val indicadores: Flow<Indicadores?> = when (resultado) {
        is Resultado.Exito -> resultado.valor
        is Resultado.Error -> flowOf(null)
    }

    val estado: StateFlow<IndicadoresUiState> = combine(
        indicadores, observarUsuarioActual(), observarEstadoDatos(), reloj
    ) { ind, usuario, datos, ahora ->
        IndicadoresUiState(
            cargando = false,
            sinPermiso = sinPermiso,
            nombre = usuario?.nombre.orEmpty(),
            cargo = usuario?.cargo.orEmpty(),
            actualizado = textoActualizado(datos.ultimaDescarga, ahora),
            indicadores = ind
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndicadoresUiState())

    private companion object {
        const val RELOJ_MS = 60_000L
    }
}
