package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstadoDatosRepositoryImpl @Inject constructor(
    monitor: MonitorConexion,
    sincronizacionDao: SincronizacionDao,
    incidenciaDao: IncidenciaDao,
    movimientoDao: MovimientoDao
) : EstadoDatosRepository {

    override val estado: Flow<EstadoDatos> = combine(
        monitor.hayConexion,
        sincronizacionDao.observarUltimaExitosa(TAREA_BAJAR),
        incidenciaDao.observarPendientes(),
        movimientoDao.observarPendientes()
    ) { enLinea, ultima, incidencias, movimientos ->
        EstadoDatos(enLinea, ultima?.ejecutadaEn, incidencias + movimientos)
    }
}
