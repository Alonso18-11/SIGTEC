package com.softcorp.sigtec.feature.incidencias.data

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Escribe solo en Room. Lo marcado con pendienteSincronizar lo sube a Firestore
 * la sincronización (HU-17): este repositorio nunca habla con Firestore.
 */
class IncidenciaRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
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

    private companion object {
        const val TAG = "IncidenciaRepository"
    }
}
