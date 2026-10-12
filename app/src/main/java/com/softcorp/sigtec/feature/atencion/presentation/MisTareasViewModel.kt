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

// Sin tipo de fila propio: la tarjeta de la bandeja usa Incidencia + EtiquetaIncidencia, igual que
// ListaIncidenciasUiState (HU-04); no hay nada reutilizable que compartir, porque TarjetaIncidencia
// de ListaIncidenciasScreen.kt es privada de ese archivo (RF-802).
data class MisTareasUiState(
    val cargando: Boolean = true,
    val incidencias: List<Incidencia> = emptyList(),   // RF-801: ya ordenadas, de la más antigua a la más reciente
    val ahora: Instant = Instant.now()                  // para los días pendientes de EtiquetaIncidencia (RF-805)
)

@HiltViewModel
class MisTareasViewModel @Inject constructor(
    observarBandeja: ObservarBandeja
) : ViewModel() {

    // «Hoy» no debe quedar desactualizado si la pantalla queda abierta mucho tiempo sin cambios de
    // datos (mismo patrón que RegistroIncidenciaViewModel, ListaIncidenciasViewModel y
    // DetalleIncidenciaViewModel, ver MEMORY.md)
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
