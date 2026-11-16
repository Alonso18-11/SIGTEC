package com.softcorp.sigtec.feature.ia.data

import android.util.Log
import com.softcorp.sigtec.core.database.dao.SugerenciaIADao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.ia.domain.SugerenciaIARepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/** HU-14: las sugerencias en Room. Una sin decidir no se sube (D-7); al decidirla queda pendiente de subir (RF-1429). */
class SugerenciaIARepositoryImpl @Inject constructor(
    private val dao: SugerenciaIADao,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : SugerenciaIARepository {

    override fun observarSinDecidir(incidenciaId: String, tipo: TipoSugerencia): Flow<SugerenciaIA?> =
        dao.observarSinDecidir(incidenciaId, tipo).map { it?.toDomain() }.flowOn(io)

    override suspend fun sinDecidir(incidenciaId: String, tipo: TipoSugerencia): Resultado<SugerenciaIA?> =
        enRoom("leer la sugerencia sin decidir") { dao.sinDecidir(incidenciaId, tipo)?.toDomain() }

    override suspend fun contar(incidenciaId: String, tipo: TipoSugerencia): Resultado<Int> =
        enRoom("contar las sugerencias") { dao.contar(incidenciaId, tipo) }

    override suspend fun guardar(sugerencia: SugerenciaIA): Resultado<Unit> =
        enRoom("guardar la sugerencia") { dao.guardar(sugerencia.toEntity(pendienteSincronizar = false)) }

    override suspend fun decidir(id: String, aceptada: Boolean, decididaPorId: String, ahora: Instant): Resultado<Unit> =
        enRoom("decidir la sugerencia") { dao.decidir(id, aceptada, decididaPorId, ahora); Unit }

    private suspend fun <T> enRoom(que: String, bloque: suspend () -> T): Resultado<T> = withContext(io) {
        try {
            Resultado.Exito(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error al $que", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    private companion object {
        const val TAG = "SugerenciaIARepository"
    }
}
