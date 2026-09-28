package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.incidencias.domain.RegistrarIncidencia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class RegistroIncidenciaUiState(
    val codigoEquipo: String = "",
    val descripcion: String = "",
    val responsable: String = "",
    val fechaHora: Instant = Instant.now(),      // solo se muestra: la fecha real la pone el caso de uso
    val registradoPor: String? = null,
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val registradaId: String? = null             // evento: se navega al detalle y se limpia
) {
    val puedeGuardar: Boolean
        get() = codigoEquipo.isNotBlank() && descripcion.isNotBlank() && responsable.isNotBlank() &&
                registradoPor != null && !guardando
}

@HiltViewModel
class RegistroIncidenciaViewModel @Inject constructor(
    private val registrarIncidencia: RegistrarIncidencia,
    observarUsuarioActual: ObservarUsuarioActual
) : ViewModel() {

    private val formulario = MutableStateFlow(RegistroIncidenciaUiState())

    // La fecha mostrada avanza sola mientras la pantalla está abierta
    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    val estado: StateFlow<RegistroIncidenciaUiState> = combine(
        formulario, observarUsuarioActual(), reloj
    ) { f, usuario, ahora ->
        f.copy(fechaHora = ahora, registradoPor = usuario?.nombre)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistroIncidenciaUiState())

    fun alCambiarCodigo(valor: String) = formulario.update { it.copy(codigoEquipo = valor, error = null) }

    fun alCambiarDescripcion(valor: String) = formulario.update { it.copy(descripcion = valor, error = null) }

    fun alCambiarResponsable(valor: String) = formulario.update { it.copy(responsable = valor, error = null) }

    fun guardar() {
        val actual = estado.value
        if (!actual.puedeGuardar) return
        formulario.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (val r = registrarIncidencia(actual.codigoEquipo, actual.descripcion, actual.responsable)) {
                is Resultado.Exito -> formulario.update { it.copy(guardando = false, registradaId = r.valor.id) }
                is Resultado.Error -> formulario.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    fun alNavegarAlDetalle() = formulario.update { it.copy(registradaId = null) }

    private companion object {
        const val RELOJ_MS = 60_000L
    }
}
