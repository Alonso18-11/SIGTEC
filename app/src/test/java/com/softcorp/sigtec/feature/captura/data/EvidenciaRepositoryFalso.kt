package com.softcorp.sigtec.feature.captura.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.feature.captura.domain.EvidenciaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Guarda en memoria lo que recibirían Room y la carpeta interna, y lo expone como Flow (HU-09).
 * Con fallar = true simula un error de Room al enlazar; como el real, entonces descarta el archivo.
 */
class EvidenciaRepositoryFalso(var fallar: Boolean = false) : EvidenciaRepository {

    private val _incidencias = MutableStateFlow<List<Incidencia>>(emptyList())
    private val _evidencias = MutableStateFlow<List<Evidencia>>(emptyList())

    val evidencias: List<Evidencia> get() = _evidencias.value

    /** Rutas cuyo archivo se borró, en orden. */
    val descartadas = mutableListOf<String>()

    /** Carga datos de prueba; también sirve para simular un cambio que llega desde otro celular. */
    fun cargar(incidencias: List<Incidencia> = _incidencias.value, evidencias: List<Evidencia> = _evidencias.value) {
        _incidencias.value = incidencias
        _evidencias.value = evidencias
    }

    override fun observarIncidencia(incidenciaId: String): Flow<Incidencia?> =
        _incidencias.map { lista -> lista.firstOrNull { it.id == incidenciaId } }

    override fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>> =
        _evidencias.map { lista -> lista.filter { it.incidenciaId == incidenciaId }.sortedBy(Evidencia::fechaCaptura) }

    override suspend fun incidencia(incidenciaId: String): Incidencia? =
        _incidencias.value.firstOrNull { it.id == incidenciaId }

    override suspend fun contarEvidencias(incidenciaId: String): Int =
        _evidencias.value.count { it.incidenciaId == incidenciaId }

    override suspend fun rutaParaNuevaFoto(incidenciaId: String, id: String): String =
        "/evidencias/$incidenciaId/$id.jpg"

    override suspend fun guardar(evidencia: Evidencia): Resultado<Unit> {
        if (fallar) {
            descartadas += evidencia.rutaArchivo
            return Resultado.Error(ErrorDominio.Desconocido("falla simulada"))
        }
        _evidencias.update { lista -> lista.filterNot { it.id == evidencia.id } + evidencia }
        return Resultado.Exito(Unit)
    }

    override suspend fun descartarArchivo(ruta: String) {
        descartadas += ruta
    }
}
