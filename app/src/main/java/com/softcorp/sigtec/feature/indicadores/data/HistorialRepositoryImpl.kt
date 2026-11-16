package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.feature.indicadores.domain.DatosHistorial
import com.softcorp.sigtec.feature.indicadores.domain.HistorialRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * HU-16 (RF-1622, RF-1626): solo lee Room. La sincronización en tiempo real trae las incidencias, los movimientos y,
 * desde esta HU, los informes de los demás celulares (RF-1623).
 */
class HistorialRepositoryImpl @Inject constructor(
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    private val catalogoDao: CatalogoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : HistorialRepository {

    override fun observarDatos(codigo: String): Flow<DatosHistorial> =
        combine(
            catalogoDao.observarEquipo(codigo),
            incidenciaDao.observarPorEquipo(codigo),
            movimientoDao.observarCierresDeEquipo(codigo),
            informeTecnicoDao.observarDeEquipo(codigo)
        ) { equipo, incidencias, cierres, informes ->
            DatosHistorial(
                equipo = equipo?.toDomain(),
                incidencias = incidencias.map { it.toDomain() },
                cierres = cierres.map { it.toDomain() },
                informes = informes.map { it.toDomain() }
            )
        }.flowOn(io)
}
