package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.EvaluadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.atencion.domain.CerrarIncidenciaConInforme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** HU-08 (RF-813 a RF-820): formulario de «Informe técnico» que cierra la incidencia confirmando con huella. */
data class InformeTecnicoUiState(
    val diagnostico: String = "",
    val trabajoRealizado: String = "",
    val piezasCambiadas: String = "",
    val observaciones: String = "",
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val disponibilidadBiometrica: DisponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD,
    val cerrada: Boolean = false   // evento de un solo uso: la pantalla navega hacia atrás y lo limpia (RF-821)
) {
    // RF-814: diagnóstico y solución obligatorios; RF-816: sin bloqueo de pantalla no se puede confirmar
    val puedeConfirmar: Boolean
        get() = diagnostico.isNotBlank() && trabajoRealizado.isNotBlank() && !guardando &&
                disponibilidadBiometrica.puedeConfirmar
}

@HiltViewModel
class InformeTecnicoViewModel : ViewModel {

    private val incidenciaId: String
    private val cerrarIncidenciaConInforme: CerrarIncidenciaConInforme
    private val _estado: MutableStateFlow<InformeTecnicoUiState>
    val estado: StateFlow<InformeTecnicoUiState>

    /**
     * Constructor que usa Hilt: calcula la disponibilidad biométrica una sola vez (igual que
     * AjustesSeguridadViewModel, único precedente de biometría hoy; D-8 del plan dice que no hace
     * falta que sea reactiva con Flow) y delega al constructor de abajo.
     */
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        evaluador: EvaluadorBiometrico
    ) : this(savedStateHandle, cerrarIncidenciaConInforme, evaluador.evaluar())

    /**
     * Constructor usado por `InformeTecnicoViewModelTest`: recibe la disponibilidad ya calculada,
     * para no depender de `EvaluadorBiometrico` (que necesita un `Context` de Android real y no se
     * puede simular en una prueba JVM sin Robolectric ni librerías de mocks, ninguna de las dos
     * disponibles en este proyecto).
     */
    constructor(
        savedStateHandle: SavedStateHandle,
        cerrarIncidenciaConInforme: CerrarIncidenciaConInforme,
        disponibilidadBiometrica: DisponibilidadBiometrica
    ) : super() {
        // Ruta.RegistrarInforme guarda su argumento con el nombre de la propiedad (mismo patrón
        // que DetalleIncidenciaViewModel.ARG_INCIDENCIA_ID)
        incidenciaId = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])
        this.cerrarIncidenciaConInforme = cerrarIncidenciaConInforme
        _estado = MutableStateFlow(InformeTecnicoUiState(disponibilidadBiometrica = disponibilidadBiometrica))
        estado = _estado.asStateFlow()
    }

    fun alCambiarDiagnostico(valor: String) = _estado.update { it.copy(diagnostico = valor, error = null) }

    fun alCambiarTrabajo(valor: String) = _estado.update { it.copy(trabajoRealizado = valor, error = null) }

    fun alCambiarPiezas(valor: String) = _estado.update { it.copy(piezasCambiadas = valor, error = null) }

    fun alCambiarObservaciones(valor: String) = _estado.update { it.copy(observaciones = valor, error = null) }

    /**
     * RF-815: `confirmadoEn` llega ya validado por `rememberVerificadorBiometrico()` desde la Route
     * (D-8 del plan); este ViewModel nunca pide la huella ni llama a Instant.now().
     */
    fun alConfirmarCierre(confirmadoEn: Instant) {
        val actual = _estado.value
        if (!actual.puedeConfirmar) return
        // "guardando" se fija antes de lanzar la corrutina: un segundo toque inmediato ya ve
        // puedeConfirmar = false y no duplica el cierre (RF-814).
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (
                val r = cerrarIncidenciaConInforme(
                    incidenciaId = incidenciaId,
                    diagnostico = actual.diagnostico,
                    trabajoRealizado = actual.trabajoRealizado,
                    piezasCambiadas = actual.piezasCambiadas,
                    observaciones = actual.observaciones,
                    confirmadoEn = confirmadoEn
                )
            ) {
                // "guardando" no vuelve a false: la pantalla se cierra al ver "cerrada" (RF-821)
                is Resultado.Exito -> _estado.update { it.copy(cerrada = true) }
                // RF-820: no se pierde el formulario; debe volver a confirmar con su huella
                is Resultado.Error -> _estado.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    private companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
    }
}
