package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.indicadores.domain.HistorialEquipo
import com.softcorp.sigtec.feature.indicadores.domain.ObservarHistorialEquipo
import com.softcorp.sigtec.feature.indicadores.domain.normalizarCodigoEquipo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class HistorialEquipoUiState(
    val modoBusqueda: Boolean = false,       // RF-1603: llegó sin código
    val texto: String = "",                  // lo escrito en «Código del equipo»
    val codigo: String? = null,              // el que se muestra; null = formulario de búsqueda
    val cargando: Boolean = false,
    val error: ErrorDominio? = null,         // RF-1602
    val historial: HistorialEquipo? = null,
    val ahora: Instant = Instant.EPOCH       // para los días de la etiqueta de cada atención
) {
    val puedeBuscar: Boolean get() = texto.isNotBlank()             // RF-1604
    val puedeVolverABuscar: Boolean get() = modoBusqueda && codigo != null
}

/** Lo que devuelve el caso de uso para el código que se muestra. */
private sealed interface Consulta {
    data object SinCodigo : Consulta
    data object Cargando : Consulta
    data class Fallida(val error: ErrorDominio) : Consulta
    data class Lista(val historial: HistorialEquipo) : Consulta
}

/**
 * HU-16 (RF-1601 a RF-1605): el historial del código que llega en la ruta, o del que se escribe o escanea en la
 * búsqueda. El texto y el código buscado viven en el [SavedStateHandle], así no se pierden al girar el celular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistorialEquipoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observarHistorial: ObservarHistorialEquipo
) : ViewModel() {

    private val codigoRuta: String? = savedStateHandle.get<String>(ARG_CODIGO_EQUIPO)?.takeIf { it.isNotBlank() }
    private val modoBusqueda = codigoRuta == null

    private val texto = savedStateHandle.getStateFlow(CLAVE_TEXTO, "")
    private val codigo = savedStateHandle.getStateFlow(CLAVE_CODIGO, codigoRuta?.let(::normalizarCodigoEquipo))

    // Los 90 días no deben quedar desactualizados si la pantalla queda abierta (mismo patrón que el tablero)
    private val reloj: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val consulta: Flow<Consulta> = codigo.flatMapLatest { c ->
        if (c == null) {
            flowOf(Consulta.SinCodigo)
        } else {
            when (val r = observarHistorial(c, reloj)) {
                is Resultado.Error -> flowOf(Consulta.Fallida(r.causa))
                is Resultado.Exito -> r.valor.map<HistorialEquipo, Consulta> { Consulta.Lista(it) }
                    .onStart { emit(Consulta.Cargando) }
            }
        }
    }

    val estado: StateFlow<HistorialEquipoUiState> = combine(texto, codigo, consulta, reloj) { t, c, q, ahora ->
        HistorialEquipoUiState(
            modoBusqueda = modoBusqueda,
            texto = t,
            codigo = c,
            cargando = q == Consulta.Cargando,
            error = (q as? Consulta.Fallida)?.error,
            historial = (q as? Consulta.Lista)?.historial,
            ahora = ahora
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistorialEquipoUiState(modoBusqueda = modoBusqueda, codigo = codigo.value, cargando = codigo.value != null)
    )

    fun alCambiarTexto(nuevo: String) {
        savedStateHandle[CLAVE_TEXTO] = nuevo
    }

    /** RF-1604: muestra el historial del código escrito, ya normalizado. */
    fun alBuscar() {
        val c = normalizarCodigoEquipo(texto.value)
        if (c.isEmpty()) return
        savedStateHandle[CLAVE_TEXTO] = c
        savedStateHandle[CLAVE_CODIGO] = c
    }

    /** RF-1605: el código que devuelve el escaneo se pone en el campo y se busca sin otro toque. */
    fun alUsarCodigoEscaneado(codigoLeido: String) {
        savedStateHandle[CLAVE_TEXTO] = codigoLeido
        alBuscar()
    }

    /** Volver desde un historial buscado lleva al formulario, con el texto que tenía. */
    fun alVolverABuscar() {
        if (modoBusqueda) savedStateHandle[CLAVE_CODIGO] = null
    }

    companion object {
        const val ARG_CODIGO_EQUIPO = "codigoEquipo"   // el nombre del parámetro de Ruta.HistorialEquipo
        private const val CLAVE_TEXTO = "historialTexto"
        private const val CLAVE_CODIGO = "historialCodigo"
        private const val RELOJ_MS = 60_000L
    }
}
