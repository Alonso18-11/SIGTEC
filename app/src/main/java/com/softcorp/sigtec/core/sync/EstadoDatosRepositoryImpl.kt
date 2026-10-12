package com.softcorp.sigtec.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstadoDatosRepositoryImpl @Inject constructor(
    monitor: MonitorConexion,
    sincronizacionDao: SincronizacionDao,
    private val baseLocal: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : EstadoDatosRepository {

    // RF-13 (HU-01): una por incidencia con algo sin subir. Sus movimientos no suman aparte:
    // siempre se guardan en la misma transacción que la incidencia, que queda pendiente con ellos.
    override val estado: Flow<EstadoDatos> = combine(
        monitor.hayConexion,
        sincronizacionDao.observarUltimaExitosa(TAREA_BAJAR),
        incidenciaDao.observarPendientes(),
        // RF-76 (D-8 de la spec 005): por incidencia afectada, igual que los cambios sin enviar
        incidenciaDao.observarAfectadasPorRechazo()
    ) { enLinea, ultima, incidencias, rechazadas ->
        EstadoDatos(enLinea, ultima?.ejecutadaEn, incidencias, necesitanAtencion = rechazadas)
    }

    // RF-80: el servidor ya ganó (RF-75, RF-79); aquí solo se limpia lo que quedó en el celular
    override suspend fun descartarRechazados(): Resultado<Unit> = withContext(io) {
        try {
            baseLocal.withTransaction {
                movimientoDao.borrarRechazados()   // primero: busca las incidencias que se van a borrar
                informeTecnicoDao.borrarRechazados()   // RF-825
                incidenciaDao.borrarRechazadasSinNumero()
                incidenciaDao.quitarMarcaDeAtencion()
            }
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron descartar los cambios rechazados", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    private companion object {
        const val TAG = "EstadoDatos"
    }
}
