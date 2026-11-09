package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import com.softcorp.sigtec.feature.incidencias.domain.DirectorioRepository
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import com.softcorp.sigtec.feature.incidencias.domain.RegistrarIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.ResultadoDirectorio
import com.softcorp.sigtec.feature.captura.domain.unirTexto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** HU-12: en qué va la consulta al directorio de colaboradores. */
sealed interface EstadoDirectorio {
    data object Inactivo : EstadoDirectorio
    data object Buscando : EstadoDirectorio                                 // RF-1203
    data class Encontrado(val colaborador: Colaborador) : EstadoDirectorio  // RF-1206, o la sugerencia de RF-1207
    data object NoEncontrado : EstadoDirectorio                             // RF-1211
    data object SinRespuesta : EstadoDirectorio                             // RF-1212
}

data class RegistroIncidenciaUiState(
    val codigoEquipo: String = "",
    val codigoLeidoConCamara: Boolean = false,   // HU-09 (RF-910): muestra «Leído desde la etiqueta con la cámara»
    val descripcion: String = "",
    val responsable: String = "",
    val directorio: EstadoDirectorio = EstadoDirectorio.Inactivo,
    val responsableDelDirectorio: Boolean = false,   // HU-12 (RF-1206, RF-1208): el campo tiene lo del directorio, sin editar
    val fechaHora: Instant = Instant.now(),      // solo se muestra: la fecha real la pone el caso de uso
    val registradoPor: String? = null,
    val guardando: Boolean = false,
    val error: ErrorDominio? = null,
    val registradaId: String? = null             // evento: se navega al detalle y se limpia
) {
    val puedeGuardar: Boolean
        get() = codigoEquipo.isNotBlank() && descripcion.isNotBlank() && responsable.isNotBlank() &&
                registradoPor != null && !guardando

    private val colaborador: Colaborador?
        get() = (directorio as? EstadoDirectorio.Encontrado)?.colaborador

    /** RF-1206: «Contabilidad, anexo 2314» bajo el campo, solo mientras el nombre sea el del directorio. */
    val areaYAnexo: String?
        get() = colaborador?.takeIf { responsableDelDirectorio }?.let { c ->
            listOfNotNull(c.area.ifBlank { null }, c.anexo?.let { "anexo $it" }).joinToString(", ").ifEmpty { null }
        }

    /** RF-1207: lo que trajo el directorio cuando el campo tiene otra cosa escrita a mano; se ofrece «Usar». */
    val sugerenciaDirectorio: Colaborador?
        get() = colaborador?.takeIf { !responsableDelDirectorio }

    /** RF-1214: el área se guarda solo si el nombre vino del directorio y nadie lo editó. */
    val areaParaGuardar: String?
        get() = colaborador?.takeIf { responsableDelDirectorio }?.area
}

/** Un pedido de consulta; [vez] distingue dos pedidos iguales (por ejemplo, «Reintentar»). */
private data class ConsultaDirectorio(val codigo: String, val inmediata: Boolean, val vez: Long)

@HiltViewModel
class RegistroIncidenciaViewModel @Inject constructor(
    private val registrarIncidencia: RegistrarIncidencia,
    observarUsuarioActual: ObservarUsuarioActual,
    private val incidencias: IncidenciaRepository,
    private val directorio: DirectorioRepository
) : ViewModel() {

    private val formulario = MutableStateFlow(RegistroIncidenciaUiState())

    private val consultas = MutableStateFlow<ConsultaDirectorio?>(null)
    private var veces = 0L

    init {
        // RF-1204: collectLatest cancela la consulta anterior cuando cambia el código
        viewModelScope.launch {
            consultas.filterNotNull().collectLatest { consultarDirectorio(it) }
        }
    }

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

    // RF-911: si lo edita a mano, ya no es lo que leyó la cámara
    fun alCambiarCodigo(valor: String) {
        val cambio = valor != formulario.value.codigoEquipo
        formulario.update { it.copy(codigoEquipo = valor, codigoLeidoConCamara = false, error = null) }
        if (cambio) pedirConsulta(valor, inmediata = false)   // HU-12 (RF-1201): tras 0,6 s sin escribir
    }

    /** HU-09 (RF-910): el código confirmado en «Escanear etiqueta» reemplaza lo que hubiera escrito. */
    fun alUsarCodigoEscaneado(codigo: String) {
        formulario.update { it.copy(codigoEquipo = codigo, codigoLeidoConCamara = true, error = null) }
        pedirConsulta(codigo, inmediata = true)   // HU-12 (RF-1202): sin esperar
    }

    fun alCambiarDescripcion(valor: String) = formulario.update { it.copy(descripcion = valor, error = null) }

    /** HU-10 (RF-1011): lo dictado se agrega al final de la descripción; durante el guardado no cambia nada (RF-1002). */
    fun alRecibirDictado(texto: String) = formulario.update {
        if (it.guardando) it else it.copy(descripcion = unirTexto(it.descripcion, texto), error = null)
    }

    // HU-12 (RF-1208): editado a mano deja de ser lo del directorio; el área se quita y queda «Usar»
    fun alCambiarResponsable(valor: String) =
        formulario.update { it.copy(responsable = valor, responsableDelDirectorio = false, error = null) }

    /** HU-12 (RF-1207, RF-1208): usa el responsable que trajo el directorio. */
    fun alUsarDelDirectorio() = formulario.update { f ->
        val c = (f.directorio as? EstadoDirectorio.Encontrado)?.colaborador
        if (c == null || f.guardando) f else f.copy(responsable = c.nombre, responsableDelDirectorio = true, error = null)
    }

    /** HU-12 (RF-1212): vuelve a consultar el código actual, sin esperar. */
    fun alReintentarDirectorio() = pedirConsulta(formulario.value.codigoEquipo, inmediata = true, vaciar = false)

    private fun pedirConsulta(codigo: String, inmediata: Boolean, vaciar: Boolean = true) {
        // RF-1209: lo que completó el directorio era de otro equipo; lo escrito a mano no se toca
        if (vaciar) formulario.update { f ->
            f.copy(
                directorio = EstadoDirectorio.Inactivo,
                responsable = if (f.responsableDelDirectorio) "" else f.responsable,
                responsableDelDirectorio = false
            )
        }
        consultas.value = ConsultaDirectorio(codigo.trim().uppercase(), inmediata, ++veces)
    }

    private suspend fun consultarDirectorio(consulta: ConsultaDirectorio) {
        if (!consulta.inmediata) delay(ESPERA_DIRECTORIO_MS)
        val codigo = consulta.codigo
        // RF-1201 (D-5): con un código incompleto o fuera del catálogo no se consulta
        if (codigo.isEmpty() || !incidencias.existeEquipo(codigo)) {
            formulario.update { it.copy(directorio = EstadoDirectorio.Inactivo) }
            return
        }
        formulario.update { it.copy(directorio = EstadoDirectorio.Buscando) }
        val nuevo = when (val r = directorio.responsableDe(codigo)) {
            is ResultadoDirectorio.Encontrado -> EstadoDirectorio.Encontrado(r.colaborador)
            is ResultadoDirectorio.Fallo -> when (r.error) {
                ErrorDirectorio.NO_ENCONTRADO -> EstadoDirectorio.NoEncontrado
                ErrorDirectorio.SIN_RESPUESTA -> EstadoDirectorio.SinRespuesta
            }
        }
        formulario.update { f ->
            // RF-1206 y RF-1207 (D-6): solo se escribe si el campo está vacío o ya era lo del directorio
            val c = (nuevo as? EstadoDirectorio.Encontrado)?.colaborador
            if (c != null && !f.guardando && (f.responsable.isBlank() || f.responsableDelDirectorio)) {
                f.copy(directorio = nuevo, responsable = c.nombre, responsableDelDirectorio = true)
            } else {
                f.copy(directorio = nuevo)
            }
        }
    }

    fun guardar() {
        val actual = estado.value
        if (!actual.puedeGuardar) return
        formulario.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (val r = registrarIncidencia(
                actual.codigoEquipo, actual.descripcion, actual.responsable, Instant.now(),
                areaResponsable = actual.areaParaGuardar   // HU-12 (RF-1214); no espera al directorio (RF-1213)
            )) {
                // "guardando" no vuelve a false: la pantalla se cierra al navegar al detalle,
                // y así un segundo toque no puede registrar la misma incidencia dos veces
                is Resultado.Exito -> formulario.update { it.copy(registradaId = r.valor.id) }
                is Resultado.Error -> formulario.update { it.copy(guardando = false, error = r.causa) }
            }
        }
    }

    fun alNavegarAlDetalle() = formulario.update { it.copy(registradaId = null) }

    private companion object {
        const val RELOJ_MS = 60_000L
        const val ESPERA_DIRECTORIO_MS = 600L   // HU-12 (RF-1201, D-5): la misma espera de la búsqueda de piezas
    }
}