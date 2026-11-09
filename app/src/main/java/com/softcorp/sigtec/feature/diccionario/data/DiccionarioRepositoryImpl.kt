package com.softcorp.sigtec.feature.diccionario.data

import android.util.Log
import com.softcorp.sigtec.core.database.dao.FallaDiccionarioDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.feature.diccionario.domain.DiccionarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DiccionarioRepositoryImpl @Inject constructor(
    private val dao: FallaDiccionarioDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : DiccionarioRepository {

    override fun observarTodas(): Flow<List<FallaDiccionario>> =
        dao.observarTodas().map { lista -> lista.map { it.toDomain() } }.flowOn(io)

    override fun existeConOrigen(incidenciaId: String): Flow<Boolean> =
        dao.existeConOrigen(incidenciaId).flowOn(io)

    override suspend fun guardar(falla: FallaDiccionario): Resultado<Unit> = withContext(io) {
        try {
            dao.guardar(falla.toEntity(pendienteSincronizar = true))
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error al guardar la falla en el diccionario", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    private companion object {
        const val TAG = "DiccionarioRepository"
    }
}
