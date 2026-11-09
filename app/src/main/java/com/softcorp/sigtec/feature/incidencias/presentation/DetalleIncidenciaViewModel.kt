package com.softcorp.sigtec.feature.incidencias.presentation

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
import com.softcorp.sigtec.feature.diccionario.domain.fallasParecidas
import com.softcorp.sigtec.feature.diccionario.domain.ObservarDiccionario
import com.softcorp.sigtec.feature.diccionario.domain.EstaEnDiccionario
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.di.DefaultDispatcher
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
    // HU-13 (RF-1324 a RF-1330): null = no se muestra la sección; vacía = «No hay fallas parecidas»
    val fallasParecidas: List<FallaDiccionario>? = null,
    val puedeGuardarEnDiccionario: Boolean = false,   // RF-1316: técnico, Solucionada y con el informe aquí
    val yaEnDiccionario: Boolean = false,             // RF-1319
    val ahora: Instant = Instant.now()
) {
    val noEncontrada: Boolean get() = !cargando && detalle == null
}

private data class AccionIniciarAtencion(val iniciando: Boolean = false, val error: ErrorDominio? = null)

private data class AccionFoto(val eliminando: Boolean = false, val error: ErrorDominio? = null)

private data class Extras(
    val foto: AccionFoto,
    val linternaEncendida: Boolean,
    val errorLinterna: Boolean,
    val fallasParecidas: List<FallaDiccionario>?,
    val yaEnDiccionario: Boolean
)

@HiltViewModel
class DetalleIncidenciaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observarDetalle: ObservarDetalleIncidencia,
    observarUsuarioActual: ObservarUsuarioActual,
    private val iniciarAtencion: IniciarAtencion,
    private val eliminarEvidencia: EliminarEvidencia,
    private val linterna: Linterna,
    observarDiccionario: ObservarDiccionario,
    estaEnDiccionario: EstaEnDiccionario,
    @DefaultDispatcher calculo: CoroutineDispatcher
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

    /**
     * HU-13 (RF-1324 a RF-1330, D-1, D-2): las fallas más parecidas a la descripción, solo con la incidencia pendiente y
     * para quien consulta el diccionario (el jefe no). Se calculan fuera del hilo principal y se recalculan si cambia el
     * diccionario o la descripción (RF-1328). No se guardan ni se suben (RF-1329).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sugerencias: Flow<List<FallaDiccionario>?> =
        observarUsuarioActual().flatMapLatest {
            when (val acceso = observarDiccionario()) {
                is Resultado.Error -> flowOf(null)
                is Resultado.Exito -> combine(
                    observarDetalle(incidenciaId)
                        .map { it?.incidencia?.let { inc -> inc.descripcion to (inc.estado == EstadoIncidencia.PENDIENTE) } }
                        .distinctUntilChanged(),
                    acceso.valor.fallas
                ) { datos, fallas ->
                    val (descripcion, pendiente) = datos ?: return@combine null
                    if (pendiente) fallasParecidas(descripcion, fallas) else null
                }
            }
        }.flowOn(calculo)

    private val extras = combine(
        accionFoto, linterna.encendida, errorLinterna, sugerencias, estaEnDiccionario(incidenciaId)
    ) { foto, encendida, error, parecidas, yaEsta ->
        Extras(foto, encendida, error, parecidas, yaEsta)
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
                fallasParecidas = extra.fallasParecidas,
                puedeGuardarEnDiccionario = incidencia?.estado == EstadoIncidencia.SOLUCIONADO &&
                        detalle.informe != null &&
                        usuario?.perfil?.puede(Permiso.REGISTRAR_FALLA) == true,
                yaEnDiccionario = extra.yaEnDiccionario,
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