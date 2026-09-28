package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.incidencias.domain.DetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class DetalleIncidenciaUiState(
    val cargando: Boolean = true,
    val detalle: DetalleIncidencia? = null,
    val puedeAsignar: Boolean = false,       // HU-01: solo el jefe ve «Asignar técnico»
    val puedeAtender: Boolean = false,       // repuesto y cierre, solo quien atiende
    val ahora: Instant = Instant.now()
) {
    val noEncontrada: Boolean get() = !cargando && detalle == null
}

@HiltViewModel
class DetalleIncidenciaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarDetalle: ObservarDetalleIncidencia,
    observarUsuarioActual: ObservarUsuarioActual
) : ViewModel() {

    // Ruta.DetalleIncidencia guarda su argumento con el nombre de la propiedad
    private val incidenciaId: String = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])

    val estado: StateFlow<DetalleIncidenciaUiState> =
        combine(observarDetalle(incidenciaId), observarUsuarioActual()) { detalle, usuario ->
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = detalle,
                puedeAsignar = usuario?.perfil?.puede(Permiso.ASIGNAR_INCIDENCIA) == true,
                puedeAtender = usuario?.perfil?.puede(Permiso.ATENDER_INCIDENCIA) == true,
                ahora = Instant.now()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIncidenciaUiState())

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
    }
}
