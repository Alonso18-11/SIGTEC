package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
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
    val puedeAsignar: Boolean = false,       // el jefe, mientras siga pendiente
    val puedeAtender: Boolean = false,       // el técnico asignado, mientras siga pendiente
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
            val incidencia = detalle?.incidencia
            val pendiente = incidencia?.estado == EstadoIncidencia.PENDIENTE
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = detalle,
                // El jefe asigna o reasigna mientras siga pendiente (HU-06)
                puedeAsignar = pendiente &&
                        usuario?.perfil?.puede(Permiso.ASIGNAR_INCIDENCIA) == true,
                // Solo el técnico asignado la atiende, y solo mientras siga pendiente (HU-08, HU-11).
                // Coincide con las reglas de Firestore: otro técnico no podría sincronizar el cambio.
                puedeAtender = pendiente &&
                        usuario != null &&
                        usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) &&
                        incidencia?.tecnicoAsignadoId == usuario.id,
                ahora = Instant.now()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIncidenciaUiState())

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
    }
}