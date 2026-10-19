package com.softcorp.sigtec.feature.incidencias.data

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.EvidenciaDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class IncidenciaRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    private val evidenciaDao: EvidenciaDao,   // HU-09: solo para leer; las fotos las escribe captura
    @IoDispatcher private val io: CoroutineDispatcher
) : IncidenciaRepository {

    override suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit> =
        withContext(io) {
            try {
                // Juntos o ninguno: nunca una incidencia sin su movimiento REGISTRADA
                db.withTransaction {
                    incidenciaDao.guardar(incidencia.toEntity(pendienteSincronizar = true))
                    movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
                }
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error al registrar la incidencia", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        }

    override fun observarTodas(): Flow<List<Incidencia>> =
        incidenciaDao.observarTodas()
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    override fun observarPorId(id: String): Flow<Incidencia?> =
        incidenciaDao.observarPorId(id)
            .map { it?.toDomain() }
            .flowOn(io)

    override fun observarMovimientos(incidenciaId: String): Flow<List<Movimiento>> =
        movimientoDao.observarPorIncidencia(incidenciaId)
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    // RF-41: true si el código ya existe en el catálogo de equipos sincronizado
    override suspend fun existeEquipo(codigo: String): Boolean =
        withContext(io) { catalogoDao.equipoPorCodigo(codigo) != null }

    // HU-08: lectura fresca para revalidar la marca justo antes de guardar (RF-807, RF-808)
    override suspend fun porId(id: String): Incidencia? =
        withContext(io) { incidenciaDao.porId(id)?.toDomain() }

    override suspend fun iniciarAtencion(
        incidencia: Incidencia,
        movimiento: Movimiento,
        ahora: Instant
    ): Resultado<Unit> =
        withContext(io) {
            try {
                db.withTransaction {
                    incidenciaDao.guardar(incidencia.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
                    movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
                }
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error al iniciar la atención de la incidencia ${incidencia.id}", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        }

    override fun observarInforme(incidenciaId: String): Flow<InformeTecnico?> =
        informeTecnicoDao.observarPorIncidencia(incidenciaId)
            .map { it?.toDomain() }
            .flowOn(io)

    override fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>> =
        evidenciaDao.observarPorIncidencia(incidenciaId)
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    private companion object {
        const val TAG = "IncidenciaRepository"
    }
}
