package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.domain.ObservarIncidencias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.Normalizer
import java.time.Instant
import javax.inject.Inject

data class FiltrosIncidencias(
    val texto: String = "",
    val marca: MarcaSeguimiento? = null,
    val tecnico: String? = null,          // nombre del técnico, o SIN_TECNICO
    val equipo: String? = null
) {
    val activos: Boolean
        get() = texto.isNotBlank() || marca != null || tecnico != null || equipo != null
}

data class ListaIncidenciasUiState(
    val cargando: Boolean = true,
    val incidencias: List<Incidencia> = emptyList(),   // ya filtradas
    val total: Int = 0,                                  // antes de filtrar
    val filtros: FiltrosIncidencias = FiltrosIncidencias(),
    val opcionesTecnico: List<String> = emptyList(),
    val opcionesEquipo: List<String> = emptyList(),
    val ahora: Instant = Instant.now()
)

@HiltViewModel
class ListaIncidenciasViewModel @Inject constructor(
    observarIncidencias: ObservarIncidencias
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosIncidencias())

    // Room ya las entrega de la más reciente a la más antigua; filtrar conserva ese orden
    val estado: StateFlow<ListaIncidenciasUiState> = combine(observarIncidencias(), filtros) { todas, f ->
        ListaIncidenciasUiState(
            cargando = false,
            incidencias = todas.filtrar(f),
            total = todas.size,
            filtros = f,
            opcionesTecnico = opcionesTecnico(todas),
            opcionesEquipo = todas.map { it.codigoEquipo }.distinct().sorted(),
            ahora = Instant.now()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaIncidenciasUiState())

    fun alBuscar(texto: String) = filtros.update { it.copy(texto = texto) }

    fun alFiltrarMarca(marca: MarcaSeguimiento?) = filtros.update { it.copy(marca = marca) }

    fun alFiltrarTecnico(tecnico: String?) = filtros.update { it.copy(tecnico = tecnico) }

    fun alFiltrarEquipo(equipo: String?) = filtros.update { it.copy(equipo = equipo) }

    fun limpiar() = filtros.update { FiltrosIncidencias() }
}

private fun opcionesTecnico(todas: List<Incidencia>): List<String> {
    val nombres = todas.mapNotNull { it.tecnicoAsignadoNombre }.distinct().sorted()
    return if (todas.any { it.tecnicoAsignadoNombre == null }) listOf(SIN_TECNICO) + nombres else nombres
}

internal fun List<Incidencia>.filtrar(f: FiltrosIncidencias): List<Incidencia> {
    val buscado = f.texto.normalizado()
    return filter { inc ->
        (buscado.isEmpty() || inc.coincideCon(buscado)) &&
                (f.marca == null || inc.marca == f.marca) &&
                (f.tecnico == null || (inc.tecnicoAsignadoNombre ?: SIN_TECNICO) == f.tecnico) &&
                (f.equipo == null || inc.codigoEquipo == f.equipo)
    }
}

// Código INC («124» también encuentra INC-0124), equipo o problema
private fun Incidencia.coincideCon(buscado: String): Boolean =
    listOf(codigoParaMostrar(), codigoEquipo, descripcion).any { it.normalizado().contains(buscado) }

// Sin mayúsculas ni tildes: «despues» encuentra «después»
private fun String.normalizado(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase()

private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
