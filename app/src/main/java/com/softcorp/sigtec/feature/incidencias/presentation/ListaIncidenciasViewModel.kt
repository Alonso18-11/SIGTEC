package com.softcorp.sigtec.feature.incidencias.presentation

import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.domain.FiltrosIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.OrdenIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.ordenar
import com.softcorp.sigtec.feature.incidencias.domain.ObservarIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.SIN_TECNICO
import com.softcorp.sigtec.feature.incidencias.domain.filtrar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject

data class ListaIncidenciasUiState(
    val cargando: Boolean = true,
    val incidencias: List<Incidencia> = emptyList(),   // ya filtradas
    val total: Int = 0,                                  // antes de filtrar
    val filtros: FiltrosIncidencias = FiltrosIncidencias(),
    val opcionesTecnico: List<String> = emptyList(),
    val opcionesEquipo: List<String> = emptyList(),
    val ahora: Instant = Instant.now(),
    val usuarioId: String? = null,  // pantallas v2: el avatar propio va en acento
    val orden: OrdenIncidencias = OrdenIncidencias.RECIENTES   // HU-14 (RF-1433): al abrir, el de la HU-04
)

@HiltViewModel
class ListaIncidenciasViewModel @Inject constructor(
    observarIncidencias: ObservarIncidencias,
    observarUsuarioActual: ObservarUsuarioActual
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosIncidencias())

    // HU-14 (RF-1435): el ViewModel sobrevive a la rotación, así que el orden también
    private val orden = MutableStateFlow(OrdenIncidencias.RECIENTES)

    // «Hoy» no debe quedar desactualizado si la pantalla queda abierta mucho tiempo sin cambios de datos
    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    // Room ya las entrega de la más reciente a la más antigua; filtrar conserva ese orden y «Más urgentes» lo cambia
    // después de filtrar (RF-1435)
    val estado: StateFlow<ListaIncidenciasUiState> =
        combine(observarIncidencias(), filtros, reloj, observarUsuarioActual(), orden) { todas, f, ahora, usuario, o ->
            ListaIncidenciasUiState(
                cargando = false,
                incidencias = todas.filtrar(f).ordenar(o),
                total = todas.size,
                filtros = f,
                opcionesTecnico = opcionesTecnico(todas),
                opcionesEquipo = todas.map { it.codigoEquipo }.distinct().sorted(),
                ahora = ahora,
                usuarioId = usuario?.id,
                orden = o
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaIncidenciasUiState())

    fun alBuscar(texto: String) = filtros.update { it.copy(texto = texto) }

    fun alFiltrarMarca(marca: MarcaSeguimiento?) = filtros.update { it.copy(marca = marca) }

    fun alFiltrarTecnico(tecnico: String?) = filtros.update { it.copy(tecnico = tecnico) }

    fun alFiltrarEquipo(equipo: String?) = filtros.update { it.copy(equipo = equipo) }

    fun alOrdenar(nuevo: OrdenIncidencias) {
        orden.value = nuevo
    }

    // RF-1435: limpiar los filtros no cambia el orden
    fun limpiar() = filtros.update { FiltrosIncidencias() }

    private companion object {
        const val RELOJ_MS = 60_000L
    }
}

private fun opcionesTecnico(todas: List<Incidencia>): List<String> {
    val nombres = todas.mapNotNull { it.tecnicoAsignadoNombre }.distinct().sorted()
    return if (todas.any { it.tecnicoAsignadoNombre == null }) listOf(SIN_TECNICO) + nombres else nombres
}
