package com.softcorp.sigtec.feature.asignacion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.asignacion.domain.AsignarIncidencia
import com.softcorp.sigtec.feature.asignacion.domain.ObservarOpcionesAsignacion
import com.softcorp.sigtec.feature.asignacion.domain.OpcionTecnico
import com.softcorp.sigtec.feature.asignacion.domain.OpcionesAsignacion
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

data class AsignacionUiState(
    val cargando: Boolean = true,
    val opciones: OpcionesAsignacion? = null,
    val elegidoId: String? = null,
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val asignadaA: String? = null,          // evento: nombre del técnico; se vuelve al detalle y se limpia
    val ahora: Instant = Instant.now()      // para los días pendientes de la etiqueta del resumen
) {
    val noEncontrada: Boolean get() = !cargando && opciones == null

    /** El elegido solo cuenta mientras se pueda elegir: si deja de tener cupo, la elección se borra. */
    val elegido: OpcionTecnico?
        get() = opciones?.tecnicos?.firstOrNull { it.tecnico.id == elegidoId && it.disponible && !it.esActual }

    val esReasignacion: Boolean get() = opciones?.incidencia?.tecnicoAsignadoId != null

    // RF-605, RF-607: con técnico elegido, sin bloqueo y sin otra asignación en curso
    val puedeAsignar: Boolean get() = elegido != null && opciones?.bloqueo == null && !guardando
}

@HiltViewModel
class AsignacionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarOpciones: ObservarOpcionesAsignacion,
    private val asignarIncidencia: AsignarIncidencia
) : ViewModel() {

    // Ruta.AsignarIncidencia guarda su argumento con el nombre de la propiedad
    private val incidenciaId: String = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])

    private val local = MutableStateFlow(AsignacionUiState())

    // Los días pendientes del resumen no deben quedar desactualizados si la pantalla queda abierta
    // mucho tiempo sin cambios de datos (mismo patrón que la lista y el detalle, HU-04)
    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    val estado: StateFlow<AsignacionUiState> = combine(observarOpciones(incidenciaId), local, reloj) { opciones, l, ahora ->
        l.copy(cargando = false, opciones = opciones, ahora = ahora)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AsignacionUiState())

    fun alElegir(tecnicoId: String) = local.update { it.copy(elegidoId = tecnicoId, error = null) }

    fun alAsignar() {
        val actual = estado.value
        val elegido = actual.elegido ?: return
        if (!actual.puedeAsignar) return
        local.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (val r = asignarIncidencia(incidenciaId, elegido.tecnico.id, Instant.now())) {
                // "guardando" no vuelve a false: la pantalla se cierra al volver al detalle,
                // y así un segundo toque no puede guardar la asignación dos veces
                is Resultado.Exito -> local.update { it.copy(asignadaA = elegido.tecnico.nombre) }
                is Resultado.Error -> local.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    fun alNavegarAlDetalle() = local.update { it.copy(asignadaA = null) }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        private const val RELOJ_MS = 60_000L
    }
}
