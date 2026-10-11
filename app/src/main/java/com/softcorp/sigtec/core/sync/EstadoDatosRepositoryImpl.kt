package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.dao.IncidenciaDao
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
    incidenciaDao: IncidenciaDao
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
}
