package com.softcorp.sigtec.feature.atencion.data

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SolicitudRepuestoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.database.entity.toEntity
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class AtencionRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    private val solicitudRepuestoDao: SolicitudRepuestoDao,
    private val programadorLogistica: ProgramadorLogistica,
    @IoDispatcher private val io: CoroutineDispatcher
) : AtencionRepository {

    override suspend fun incidencia(id: String): Incidencia? =
        withContext(io) { incidenciaDao.porId(id)?.toDomain() }

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

    // HU-11 (RF-1115): o se guardan las tres cosas, o ninguna
    override suspend fun solicitarRepuesto(
        incidencia: Incidencia,
        movimiento: Movimiento,
        solicitud: SolicitudRepuesto,
        ahora: Instant
    ): Resultado<Unit> =
        withContext(io) {
            try {
                db.withTransaction {
                    incidenciaDao.guardar(incidencia.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
                    movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
                    solicitudRepuestoDao.guardar(solicitud.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
                }
                programadorLogistica.registrarPendientes()   // RF-1117: en cuanto haya conexión
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error al guardar la solicitud de repuesto de ${incidencia.id}", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        }

    override suspend fun solicitudesPorRegistrar(): List<SolicitudRepuesto> =
        withContext(io) { solicitudRepuestoDao.sinNumeroDeLogistica().map { it.toDomain() } }

    override suspend fun marcarRegistrada(solicitudId: String, numero: String, ahora: Instant): Resultado<Unit> =
        escribir("marcar registrada la solicitud $solicitudId") {
            solicitudRepuestoDao.marcarRegistrada(solicitudId, numero, ahora)
        }

    override suspend fun marcarRechazada(solicitudId: String, ahora: Instant): Resultado<Unit> =
        escribir("marcar rechazada la solicitud $solicitudId") {
            solicitudRepuestoDao.marcarRechazada(solicitudId, ahora)
        }

    private suspend fun escribir(que: String, bloque: suspend () -> Unit): Resultado<Unit> = withContext(io) {
        try {
            bloque()
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error al $que", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    private companion object {
        const val TAG = "AtencionRepository"
    }
}
