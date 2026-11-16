package com.softcorp.sigtec.feature.asignacion.presentation

import com.softcorp.sigtec.feature.asignacion.domain.recomendado
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.asignacion.domain.ClasificacionElegida
import com.softcorp.sigtec.feature.asignacion.domain.AsignarIncidencia
import com.softcorp.sigtec.feature.asignacion.domain.ObservarOpcionesAsignacion
import com.softcorp.sigtec.feature.asignacion.domain.OpcionTecnico
import com.softcorp.sigtec.feature.asignacion.domain.OpcionesAsignacion
import com.softcorp.sigtec.feature.ia.domain.ClasificacionSugerida
import com.softcorp.sigtec.feature.ia.domain.DecidirSugerencia
import com.softcorp.sigtec.feature.ia.domain.ObservarSugerencia
import com.softcorp.sigtec.feature.ia.domain.SugerirClasificacion
import com.softcorp.sigtec.feature.ia.domain.clasificacionAceptada
import com.softcorp.sigtec.feature.ia.domain.leerClasificacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    val ahora: Instant = Instant.now(),     // para los días pendientes de la etiqueta del resumen
    // HU-14 (RF-1408): empiezan con la clasificación de la incidencia; null es «Sin clasificar»
    val tipoElegido: TipoFalla? = null,
    val prioridadElegida: Prioridad? = null,
    val clasificacionLista: Boolean = false,    // ya tomó la de la incidencia: desde ahí se envía al asignar
    // HU-14 (RF-1403 a RF-1410): la sugerencia sin decidir y lo que el jefe hizo con ella
    val generandoSugerencia: Boolean = false,
    val sugerencia: SugerenciaIA? = null,
    val sugerenciaUsada: Boolean = false,
    val sugerenciaDescartada: Boolean = false
) {
    val clasificacionSugerida: ClasificacionSugerida? get() = sugerencia?.let { leerClasificacion(it.contenido) }

    // RF-1409, RF-1410: «Usar» y «Descartar» la ocultan; sin sugerencia ni pedido en curso no se reserva espacio
    val tarjetaSugerenciaVisible: Boolean
        get() = !sugerenciaUsada && !sugerenciaDescartada && (generandoSugerencia || clasificacionSugerida != null)

    val noEncontrada: Boolean get() = !cargando && opciones == null

    /** El elegido solo cuenta mientras se pueda elegir: si deja de tener cupo, la elección se borra. */
    val elegido: OpcionTecnico?
        get() = opciones?.tecnicos?.firstOrNull { it.tecnico.id == elegidoId && it.disponible && !it.esActual }

    val esReasignacion: Boolean get() = opciones?.incidencia?.tecnicoAsignadoId != null

    // Pantallas v2 (prototipo 07): solo con la asignación permitida tiene sentido recomendar
    val recomendadoId: String? get() = opciones?.takeIf { it.bloqueo == null }?.let { recomendado(it.tecnicos) }

    // RF-605, RF-607: con técnico elegido, sin bloqueo y sin otra asignación en curso
    val puedeAsignar: Boolean get() = elegido != null && opciones?.bloqueo == null && !guardando
}

@HiltViewModel
class AsignacionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observarOpciones: ObservarOpcionesAsignacion,
    private val asignarIncidencia: AsignarIncidencia,
    private val sugerirClasificacion: SugerirClasificacion,
    observarSugerencia: ObservarSugerencia,
    private val decidirSugerencia: DecidirSugerencia
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

    // RF-1405: la sugerencia que se muestra es la guardada, así una pedida antes también aparece
    val estado: StateFlow<AsignacionUiState> = combine(
        observarOpciones(incidenciaId), local, reloj, observarSugerencia(incidenciaId, TipoSugerencia.CLASIFICACION)
    ) { opciones, l, ahora, sugerencia ->
        l.copy(cargando = false, opciones = opciones, ahora = ahora, sugerencia = sugerencia)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AsignacionUiState())

    init {
        // HU-14 (RF-1408): una sola vez, para no pisar lo que el jefe elija después
        viewModelScope.launch {
            val incidencia = observarOpciones(incidenciaId).filterNotNull().first().incidencia
            local.update {
                it.copy(tipoElegido = incidencia.tipoFalla, prioridadElegida = incidencia.prioridad, clasificacionLista = true)
            }
            // RF-1401, RF-1407: solo para una pendiente sin clasificar. Cualquier error se calla (RF-1406)
            if (incidencia.marca != MarcaSeguimiento.CERRADA && incidencia.tipoFalla == null && incidencia.prioridad == null) {
                local.update { it.copy(generandoSugerencia = true) }
                sugerirClasificacion(incidencia, Instant.now())
                local.update { it.copy(generandoSugerencia = false) }
            }
        }
    }

    fun alElegir(tecnicoId: String) = local.update { it.copy(elegidoId = tecnicoId, error = null) }

    fun alElegirTipo(tipo: TipoFalla?) = local.update { it.copy(tipoElegido = tipo) }

    fun alElegirPrioridad(prioridad: Prioridad?) = local.update { it.copy(prioridadElegida = prioridad) }

    /** RF-1409: pone lo sugerido en «Clasificación»; el jefe todavía puede cambiarlo antes de asignar. */
    fun alUsarSugerencia() {
        val sugerida = estado.value.clasificacionSugerida ?: return
        local.update {
            it.copy(tipoElegido = sugerida.tipo, prioridadElegida = sugerida.prioridad, sugerenciaUsada = true)
        }
    }

    /** RF-1410: la oculta sin tocar «Clasificación». */
    fun alDescartarSugerencia() = local.update { it.copy(sugerenciaDescartada = true) }

    fun alAsignar() {
        val actual = estado.value
        val elegido = actual.elegido ?: return
        if (!actual.puedeAsignar) return
        local.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            // HU-14 (RF-1411, RF-1412): opcional; va en la misma transacción que la asignación
            val clasificacion = ClasificacionElegida(actual.tipoElegido, actual.prioridadElegida)
                .takeIf { actual.clasificacionLista }
            when (val r = asignarIncidencia(incidenciaId, elegido.tecnico.id, Instant.now(), clasificacion)) {
                // "guardando" no vuelve a false: la pantalla se cierra al volver al detalle,
                // y así un segundo toque no puede guardar la asignación dos veces
                is Resultado.Exito -> {
                    registrarDecision(actual)    // antes del evento: al volver al detalle se cierra la pantalla
                    local.update { it.copy(asignadaA = elegido.tecnico.nombre) }
                }
                is Resultado.Error -> local.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    fun alNavegarAlDetalle() = local.update { it.copy(asignadaA = null) }

    /**
     * RF-1413 (D-4, P-8): después de asignar, la sugerencia sin decidir queda aceptada solo si se usó y se asignó tal
     * cual. Si esto falla, la asignación ya está hecha y la sugerencia queda sin decidir: no se sube ni cambia datos.
     */
    private suspend fun registrarDecision(actual: AsignacionUiState) {
        val sugerencia = actual.sugerencia ?: return
        val sugerida = actual.clasificacionSugerida ?: return
        val aceptada = clasificacionAceptada(
            sugerida, actual.sugerenciaUsada, actual.sugerenciaDescartada, actual.tipoElegido, actual.prioridadElegida
        )
        decidirSugerencia(sugerencia, aceptada, Instant.now())
    }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        private const val RELOJ_MS = 60_000L
    }
}
