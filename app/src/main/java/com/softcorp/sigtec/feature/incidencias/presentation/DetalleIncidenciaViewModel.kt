package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.incidencias.domain.DetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.IniciarAtencion
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class DetalleIncidenciaUiState(
    val cargando: Boolean = true,
    val detalle: DetalleIncidencia? = null,
    val puedeAsignar: Boolean = false,       // el jefe, mientras siga pendiente
    val puedeAtender: Boolean = false,       // el técnico asignado, mientras siga pendiente
    val puedeIniciarAtencion: Boolean = false, // HU-08: técnico asignado, solo en marca Asignada (RF-806)
    val iniciandoAtencion: Boolean = false,    // HU-08: RF-811, evita el doble toque
    val errorAtencion: ErrorDominio? = null,   // HU-08: RF-808, RF-810
    val ahora: Instant = Instant.now()
) {
    val noEncontrada: Boolean get() = !cargando && detalle == null
}

/** HU-08: estado local de «Iniciar atención», aparte del Flow de Room para que un doble toque no lo reinicie. */
private data class AccionIniciarAtencion(val iniciando: Boolean = false, val error: ErrorDominio? = null)

@HiltViewModel
class DetalleIncidenciaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarDetalle: ObservarDetalleIncidencia,
    observarUsuarioActual: ObservarUsuarioActual,
    private val iniciarAtencion: IniciarAtencion
) : ViewModel() {

    // Ruta.DetalleIncidencia guarda su argumento con el nombre de la propiedad
    private val incidenciaId: String = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])

    // «Hoy» no debe quedar desactualizado si la pantalla queda abierta mucho tiempo sin cambios de datos
    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    private val accionIniciarAtencion = MutableStateFlow(AccionIniciarAtencion())

    val estado: StateFlow<DetalleIncidenciaUiState> =
        combine(
            observarDetalle(incidenciaId),
            observarUsuarioActual(),
            reloj,
            accionIniciarAtencion
        ) { detalle, usuario, ahora, accion ->
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
                // RF-806: solo en la marca Asignada, deja de mostrarse en cuanto cambia por otra vía (D-7)
                puedeIniciarAtencion = incidencia?.marca == MarcaSeguimiento.ASIGNADA &&
                        usuario != null &&
                        usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) &&
                        incidencia.tecnicoAsignadoId == usuario.id,
                iniciandoAtencion = accion.iniciando,
                errorAtencion = accion.error,
                ahora = ahora
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIncidenciaUiState())

    /** RF-807 a RF-811: el ahora lo decide quien invoca, nunca el caso de uso. */
    fun alIniciarAtencion() {
        if (accionIniciarAtencion.value.iniciando) return   // RF-811: ya se está guardando
        accionIniciarAtencion.update { it.copy(iniciando = true, error = null) }
        viewModelScope.launch {
            when (val r = iniciarAtencion(incidenciaId, Instant.now())) {
                // "iniciando" queda en true: la marca ya cambia y el botón desaparece solo (D-7)
                is Resultado.Exito -> Unit
                is Resultado.Error -> accionIniciarAtencion.update { it.copy(iniciando = false, error = r.causa) }
            }
        }
    }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        private const val RELOJ_MS = 60_000L
    }
}