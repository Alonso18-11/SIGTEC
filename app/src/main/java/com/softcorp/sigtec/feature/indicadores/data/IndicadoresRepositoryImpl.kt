package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.feature.indicadores.domain.DatosIndicadores
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** HU-15 (RF-1504, RF-1524): solo lee Room; la sincronización en tiempo real trae los cambios. */
class IndicadoresRepositoryImpl @Inject constructor(
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : IndicadoresRepository {

    override fun observarDatos(): Flow<DatosIndicadores> =
        combine(
            incidenciaDao.observarTodas(),
            movimientoDao.observarCierres(),
            catalogoDao.observarTecnicos(),
            incidenciaDao.observarAsignacionesSinSubir(),
            catalogoDao.observarEquipos()
        ) { incidencias, cierres, tecnicos, sinSubir, equipos ->
            DatosIndicadores(
                incidencias = incidencias.map { it.toDomain() },
                cierres = cierres.map { it.toDomain() },
                tecnicos = tecnicos.map { it.toDomain() },
                // RF-1518: la consulta da un id de técnico por asignación sin subir; aquí se cuentan
                asignacionesSinSubir = sinSubir.groupingBy { it }.eachCount(),
                equipos = equipos.map { it.toDomain() }
            )
        }.flowOn(io)
}
