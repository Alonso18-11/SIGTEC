package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.captura.domain.EliminarEvidencia
import com.softcorp.sigtec.feature.captura.domain.Linterna
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
    val eliminandoFoto: Boolean = false,       // HU-09: RF-937, evita el doble toque
    val errorFoto: ErrorDominio? = null,       // HU-09: RF-937
    val linternaVisible: Boolean = false,      // HU-10: RF-1024, técnico asignado, pendiente y con flash
    val linternaEncendida: Boolean = false,    // HU-10: RF-1025, RF-1029, el estado real del flash
    val errorLinterna: Boolean = false,        // HU-10: RF-1028, evento de un solo uso
    val ahora: Instant = Instant.now()
) {
    val noEncontrada: Boolean get() = !cargando && detalle == null
}

private data class AccionIniciarAtencion(val iniciando: Boolean = false, val error: ErrorDominio? = null)

private data class AccionFoto(val eliminando: Boolean = false, val error: ErrorDominio? = null)

private data class Extras(val foto: AccionFoto, val linternaEncendida: Boolean, val errorLinterna: Boolean)

@HiltViewModel
class DetalleIncidenciaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarDetalle: ObservarDetalleIncidencia,
    observarUsuarioActual: ObservarUsuarioActual,
    private val iniciarAtencion: IniciarAtencion,
    private val eliminarEvidencia: EliminarEvidencia,
    private val linterna: Linterna
) : ViewModel() {

    private val incidenciaId: String = checkNotNull(savedStateHandle[ARG_INCIDENCIA_ID])

    private val reloj = flow {
        while (true) {
            emit(Instant.now())
            delay(RELOJ_MS)
        }
    }

    private val accionIniciarAtencion = MutableStateFlow(AccionIniciarAtencion())
    private val accionFoto = MutableStateFlow(AccionFoto())
    private val errorLinterna = MutableStateFlow(false)

    // Solo se apaga la linterna que encendió este detalle: si la encendieron desde los ajustes rápidos, no es suya
    private var encendioLaLinterna = false

    private val extras = combine(accionFoto, linterna.encendida, errorLinterna) { foto, encendida, error ->
        Extras(foto, encendida, error)
    }

    val estado: StateFlow<DetalleIncidenciaUiState> =
        combine(
            observarDetalle(incidenciaId),
            observarUsuarioActual(),
            reloj,
            accionIniciarAtencion,
            extras
        ) { detalle, usuario, ahora, accion, extra ->
            val foto = extra.foto
            val incidencia = detalle?.incidencia
            val pendiente = incidencia?.estado == EstadoIncidencia.PENDIENTE
            val puedeAtender = pendiente &&
                    usuario != null &&
                    usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) &&
                    incidencia?.tecnicoAsignadoId == usuario.id
            val linternaVisible = puedeAtender && linterna.tieneFlash
            // RF-1027: si se cierra o se reasigna con la linterna encendida, se apaga y el botón se va
            if (!linternaVisible) apagarSiLaEncendio()
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = detalle,
                // El jefe asigna o reasigna mientras siga pendiente
                puedeAsignar = pendiente &&
                        usuario?.perfil?.puede(Permiso.ASIGNAR_INCIDENCIA) == true,
                // Solo el técnico asignado la atiende, y solo mientras siga pendiente
                puedeAtender = puedeAtender,
                // Solo en la marca Asignada, deja de mostrarse en cuanto cambia por otra vía
                puedeIniciarAtencion = incidencia?.marca == MarcaSeguimiento.ASIGNADA &&
                        usuario != null &&
                        usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) &&
                        incidencia.tecnicoAsignadoId == usuario.id,
                iniciandoAtencion = accion.iniciando,
                errorAtencion = accion.error,
                eliminandoFoto = foto.eliminando,
                errorFoto = foto.error,
                linternaVisible = linternaVisible,
                linternaEncendida = extra.linternaEncendida,
                errorLinterna = extra.errorLinterna,
                ahora = ahora
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIncidenciaUiState())

    fun alIniciarAtencion() {
        if (accionIniciarAtencion.value.iniciando) return
        accionIniciarAtencion.update { it.copy(iniciando = true, error = null) }
        viewModelScope.launch {
            when (val r = iniciarAtencion(incidenciaId, Instant.now())) {
                is Resultado.Exito -> Unit
                is Resultado.Error -> accionIniciarAtencion.update { it.copy(iniciando = false, error = r.causa) }
            }
        }
    }

    /** RF-937: al eliminarla, la foto desaparece de Room y su vista ampliada se cierra sola. */
    fun alEliminarFoto(evidencia: Evidencia) {
        if (accionFoto.value.eliminando) return
        accionFoto.value = AccionFoto(eliminando = true)
        viewModelScope.launch {
            accionFoto.value = AccionFoto(error = (eliminarEvidencia(evidencia) as? Resultado.Error)?.causa)
        }
    }

    /** Al cerrar la foto ampliada, el error de ese intento ya no aplica. */
    fun alCerrarFoto() {
        accionFoto.update { it.copy(error = null) }
    }

    /** HU-10 (RF-1025, RF-1028): enciende o apaga; si no enciende, avisa una vez. */
    fun alAlternarLinterna() {
        if (linterna.encendida.value) {
            linterna.apagar()
            encendioLaLinterna = false
        } else {
            encendioLaLinterna = linterna.encender()
            errorLinterna.value = !encendioLaLinterna
        }
    }

    fun alAvisoLinternaMostrado() {
        errorLinterna.value = false
    }

    /** HU-10 (RF-1026): al salir del detalle, pasar a segundo plano o bloquear el celular. */
    fun alDejarDeVer() = apagarSiLaEncendio()

    override fun onCleared() = apagarSiLaEncendio()

    private fun apagarSiLaEncendio() {
        if (!encendioLaLinterna) return
        encendioLaLinterna = false
        linterna.apagar()
    }

    companion object {
        const val ARG_INCIDENCIA_ID = "incidenciaId"
        private const val RELOJ_MS = 60_000L
    }
}