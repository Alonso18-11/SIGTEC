package com.softcorp.sigtec.feature.asignacion.data

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.feature.asignacion.domain.AsignacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Escribe solo en Room. La incidencia asignada queda con pendienteSincronizar y la sube core/sync,
 * que además mueve la carga de los técnicos en el servidor (RF-620): este repositorio nunca habla
 * con Firestore.
 */
class AsignacionRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : AsignacionRepository {

    override suspend fun incidencia(id: String): Incidencia? =
        withContext(io) { incidenciaDao.porId(id)?.toDomain() }

    override fun observarIncidencia(id: String): Flow<Incidencia?> =
        incidenciaDao.observarPorId(id)
            .map { it?.toDomain() }
            .flowOn(io)

    override suspend fun tecnico(id: String): Tecnico? =
        withContext(io) { catalogoDao.tecnicoPorId(id)?.toDomain() }

    override fun observarTecnicos(): Flow<List<Tecnico>> =
        catalogoDao.observarTecnicos()
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun oficinaDeEquipo(codigo: String): String? =
        withContext(io) { catalogoDao.equipoPorCodigo(codigo)?.oficina }

    // RF-602: la consulta devuelve un id de técnico por asignación; aquí se cuentan por técnico
    override fun observarAsignacionesSinSubir(): Flow<Map<String, Int>> =
        incidenciaDao.observarAsignacionesSinSubir()
            .map { ids -> ids.groupingBy { it }.eachCount() }
            .flowOn(io)

    override suspend fun asignacionesSinSubir(tecnicoId: String): Int =
        withContext(io) { incidenciaDao.asignacionesSinSubir(tecnicoId) }

    override suspend fun asignar(incidencia: Incidencia, movimiento: Movimiento, ahora: Instant): Resultado<Unit> =
        withContext(io) {
            try {
                // RF-613: juntos o ninguno; nunca una asignación sin su movimiento, ni al revés
                db.withTransaction {
                    incidenciaDao.guardar(incidencia.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
                    movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
                }
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error al asignar la incidencia ${incidencia.id}", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        }

    private companion object {
        const val TAG = "AsignacionRepository"
    }
}
