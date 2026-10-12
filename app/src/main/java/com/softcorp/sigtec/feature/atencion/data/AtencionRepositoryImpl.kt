package com.softcorp.sigtec.feature.atencion.data

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Escribe solo en Room (RF-824); subir a Firestore y restar la carga del técnico es trabajo
 * exclusivo de core/sync. Nace aparte de IncidenciaRepositoryImpl (D-1 del plan 008), igual que
 * AsignacionRepositoryImpl en la HU-06, aunque toque las mismas tablas.
 */
class AtencionRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : AtencionRepository {

    // D-6 del plan: lectura fresca, para revalidar la marca justo antes de guardar (RF-812)
    override suspend fun incidencia(id: String): Incidencia? =
        withContext(io) { incidenciaDao.porId(id)?.toDomain() }

    // RF-801: solo las del técnico que no estén cerradas, de la más antigua a la más reciente
    override fun observarBandeja(tecnicoId: String): Flow<List<Incidencia>> =
        incidenciaDao.observarBandeja(tecnicoId)
            .map { lista -> lista.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun cerrarConInforme(
        incidencia: Incidencia,
        movimiento: Movimiento,
        informe: InformeTecnico,
        ahora: Instant
    ): Resultado<Unit> =
        withContext(io) {
            try {
                // Los tres o ninguno: nunca falta el informe, el movimiento o el cambio de marca (RF-819)
                db.withTransaction {
                    incidenciaDao.guardar(incidencia.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
                    movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
                    informeTecnicoDao.guardar(informe.toEntity(pendienteSincronizar = true))
                }
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error al cerrar la incidencia ${incidencia.id}", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        }

    private companion object {
        const val TAG = "AtencionRepository"
    }
}
