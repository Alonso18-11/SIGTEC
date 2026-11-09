package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.di.DefaultDispatcher
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.feature.diccionario.domain.ObservarDiccionario
import com.softcorp.sigtec.feature.diccionario.domain.filtrarFallas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DiccionarioUiState(
    val cargando: Boolean = true,
    val sinPermiso: Boolean = false,     // RF-1308: el jefe, si llegara a la pantalla
    val total: Int = 0,                  // RF-1301: cuántas hay registradas, sin filtrar
    val fallas: List<FallaDiccionario> = emptyList(),   // las que pasan la búsqueda y el filtro
    val texto: String = "",
    val tipo: TipoFalla? = null,         // null = «Todas»
    val puedeRegistrar: Boolean = false, // RF-1309: solo el técnico
    val desplegadaId: String? = null     // RF-1303
) {
    val hayFiltro: Boolean get() = texto.isNotBlank() || tipo != null
}

private data class Filtros(val texto: String = "", val tipo: TipoFalla? = null, val desplegadaId: String? = null)

/** HU-13 (RF-1301 a RF-1309): el diccionario de fallas con su búsqueda y su filtro, leído de Room. */
@HiltViewModel
class DiccionarioViewModel @Inject constructor(
    observarDiccionario: ObservarDiccionario,
    @DefaultDispatcher private val calculo: CoroutineDispatcher
) : ViewModel() {

    private val filtros = MutableStateFlow(Filtros())

    val estado: StateFlow<DiccionarioUiState> = when (val acceso = observarDiccionario()) {
        is Resultado.Error -> flowOf(DiccionarioUiState(cargando = false, sinPermiso = true))
        is Resultado.Exito -> combine(acceso.valor.fallas, filtros) { todas, f ->
            // RF-1306: en memoria y fuera del hilo principal
            DiccionarioUiState(
                cargando = false,
                total = todas.size,
                fallas = filtrarFallas(todas, f.texto, f.tipo),
                texto = f.texto,
                tipo = f.tipo,
                puedeRegistrar = acceso.valor.puedeRegistrar,
                desplegadaId = f.desplegadaId
            )
        }.flowOn(calculo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiccionarioUiState())

    fun alBuscar(texto: String) = filtros.update { it.copy(texto = texto) }

    fun alFiltrarTipo(tipo: TipoFalla?) = filtros.update { it.copy(tipo = tipo) }

    fun alTocarFalla(id: String) =
        filtros.update { it.copy(desplegadaId = if (it.desplegadaId == id) null else id) }
}
