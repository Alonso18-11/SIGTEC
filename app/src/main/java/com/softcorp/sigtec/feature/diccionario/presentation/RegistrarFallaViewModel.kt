package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.unirTexto
import com.softcorp.sigtec.feature.diccionario.domain.DatosFalla
import com.softcorp.sigtec.feature.diccionario.domain.RegistrarFalla
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class RegistrarFallaUiState(
    val titulo: String = "",
    val tipo: TipoFalla? = null,
    val componente: String = "",
    val sintomas: String = "",
    val solucion: String = "",
    val origen: String? = null,          // RF-1317: «INC-0013 - PC-CONT-014» si se abrió desde una incidencia
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val guardada: Boolean = false        // evento: se vuelve al diccionario o al detalle y se limpia
) {
    // RF-1312: los cinco campos, sin contar los espacios
    val puedeGuardar: Boolean
        get() = !guardando && tipo != null &&
            listOf(titulo, componente, sintomas, solucion).all { it.isNotBlank() }
}

/** HU-13 (RF-1310 a RF-1318): el formulario de una falla nueva, vacío o lleno desde una incidencia cerrada. */
@HiltViewModel
class RegistrarFallaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registrarFalla: RegistrarFalla,
    private val observarDetalle: ObservarDetalleIncidencia
) : ViewModel() {

    // Ruta.RegistrarFalla guarda su argumento con el nombre de la propiedad
    private val incidenciaOrigenId: String? = savedStateHandle[ARG_INCIDENCIA_ORIGEN_ID]

    private val _estado = MutableStateFlow(RegistrarFallaUiState())
    val estado: StateFlow<RegistrarFallaUiState> = _estado.asStateFlow()

    init {
        incidenciaOrigenId?.let(::llenarDesdeIncidencia)
    }

    // RF-1317: título = descripción, síntomas = diagnóstico, solución = trabajo realizado, tipo si lo tiene
    private fun llenarDesdeIncidencia(id: String) = viewModelScope.launch {
        val detalle = observarDetalle(id).first() ?: return@launch
        val incidencia = detalle.incidencia
        val informe = detalle.informe
        _estado.update {
            it.copy(
                titulo = it.titulo.ifEmpty { incidencia.descripcion },
                tipo = it.tipo ?: incidencia.tipoFalla,
                sintomas = it.sintomas.ifEmpty { informe?.diagnostico.orEmpty() },
                solucion = it.solucion.ifEmpty { informe?.trabajoRealizado.orEmpty() },
                origen = "${incidencia.codigoVisible ?: POR_SINCRONIZAR} - ${incidencia.codigoEquipo}"
            )
        }
    }

    fun alCambiarTitulo(texto: String) = _estado.update { it.copy(titulo = texto, error = null) }
    fun alCambiarTipo(tipo: TipoFalla?) = _estado.update { it.copy(tipo = tipo, error = null) }
    fun alCambiarComponente(texto: String) = _estado.update { it.copy(componente = texto, error = null) }
    fun alCambiarSintomas(texto: String) = _estado.update { it.copy(sintomas = texto, error = null) }
    fun alCambiarSolucion(texto: String) = _estado.update { it.copy(solucion = texto, error = null) }

    /** RF-1311: el dictado se agrega al final de lo escrito, como en el informe (HU-10). */
    fun alRecibirDictado(campo: CampoDictado, texto: String) = _estado.update {
        when (campo) {
            CampoDictado.SINTOMAS -> it.copy(sintomas = unirTexto(it.sintomas, texto), error = null)
            CampoDictado.SOLUCION -> it.copy(solucion = unirTexto(it.solucion, texto), error = null)
            else -> it
        }
    }

    fun alGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val datos = DatosFalla(actual.titulo, actual.tipo, actual.componente, actual.sintomas, actual.solucion)
            when (val r = registrarFalla(datos, Instant.now(), incidenciaOrigenId)) {
                // "guardando" no vuelve a false: la pantalla se cierra y un segundo toque no guarda dos veces
                is Resultado.Exito -> _estado.update { it.copy(guardada = true) }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    fun alSalir() = _estado.update { it.copy(guardada = false) }

    companion object {
        const val ARG_INCIDENCIA_ORIGEN_ID = "incidenciaOrigenId"
    }
}
