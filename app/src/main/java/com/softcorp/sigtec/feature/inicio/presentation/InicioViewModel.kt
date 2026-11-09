package com.softcorp.sigtec.feature.inicio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.inicio.domain.ObservarResumenInicio
import com.softcorp.sigtec.feature.inicio.domain.ResumenInicio
import com.softcorp.sigtec.feature.inicio.domain.saludo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

data class InicioUiState(
    val cargando: Boolean = true,
    val nombre: String = "",            // nombre completo, para el avatar
    val primerNombre: String = "",
    val cargo: String = "",
    val saludo: String = "",            // RF-I1
    val resumen: ResumenInicio? = null  // null sin sesión (RF-I12)
)

/** Inicio (specs/inicio-panel): saludo, métricas y lo que se destaca, según el perfil. */
@HiltViewModel
class InicioViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActual,
    observarResumen: ObservarResumenInicio
) : ViewModel() {

    // El saludo cambia solo si la pantalla queda abierta al pasar de la mañana a la tarde (mismo patrón que el registro)
    private val hora = flow {
        while (true) {
            emit(LocalTime.now().hour)
            delay(RELOJ_MS)
        }
    }

    val estado: StateFlow<InicioUiState> = combine(observarUsuarioActual(), observarResumen(), hora) { usuario, resumen, h ->
        InicioUiState(
            cargando = false,
            nombre = usuario?.nombre.orEmpty(),
            primerNombre = usuario?.nombre?.substringBefore(' ').orEmpty(),
            cargo = usuario?.cargo.orEmpty(),
            saludo = saludo(h),
            resumen = resumen
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    private companion object {
        const val RELOJ_MS = 60_000L
    }
}
