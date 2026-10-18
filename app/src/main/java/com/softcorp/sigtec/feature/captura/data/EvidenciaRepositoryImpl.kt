package com.softcorp.sigtec.feature.captura.data

import android.util.Log
import com.softcorp.sigtec.core.data.AlmacenFotos
import com.softcorp.sigtec.core.database.dao.EvidenciaDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.feature.captura.domain.EvidenciaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * HU-09: las fotos quedan en Room (la fila) y en la carpeta interna (el archivo), solo en este celular.
 * Nunca habla con Firestore: las evidencias no se sincronizan (RF-939).
 */
class EvidenciaRepositoryImpl @Inject constructor(
    private val incidenciaDao: IncidenciaDao,
    private val evidenciaDao: EvidenciaDao,
    private val almacen: AlmacenFotos,
    @IoDispatcher private val io: CoroutineDispatcher
) : EvidenciaRepository {

    override fun observarIncidencia(incidenciaId: String): Flow<Incidencia?> =
        incidenciaDao.observarPorId(incidenciaId)
            .map { it?.toDomain() }
            .flowOn(io)

    override fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>> =
        evidenciaDao.observarPorIncidencia(incidenciaId)
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun incidencia(incidenciaId: String): Incidencia? =
        withContext(io) { incidenciaDao.porId(incidenciaId)?.toDomain() }

    // Una incidencia tiene 10 fotos como mucho (RF-929): leer la lista basta, sin una consulta nueva
    override suspend fun contarEvidencias(incidenciaId: String): Int =
        withContext(io) { evidenciaDao.observarPorIncidencia(incidenciaId).first().size }

    override suspend fun rutaParaNuevaFoto(incidenciaId: String, id: String): String =
        withContext(io) { almacen.archivoNuevo(incidenciaId, id).absolutePath }

    // RF-928: si la fila no se guarda, el archivo se borra para no dejar fotos sueltas
    override suspend fun guardar(evidencia: Evidencia): Resultado<Unit> = withContext(io) {
        try {
            evidenciaDao.guardar(evidencia.toEntity())
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            almacen.borrar(evidencia.rutaArchivo)
            throw e
        } catch (e: Exception) {
            almacen.borrar(evidencia.rutaArchivo)
            Log.w(TAG, "No se pudo enlazar la foto ${evidencia.id}", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    override suspend fun descartarArchivo(ruta: String): Unit = withContext(io) {
        try {
            almacen.borrar(ruta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo borrar la foto $ruta", e)   // queda suelta, pero sin fila no se ve
        }
    }

    private companion object {
        const val TAG = "EvidenciaRepository"
    }
}
