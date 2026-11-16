package com.softcorp.sigtec.feature.indicadores.data

import android.util.Log
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.indicadores.domain.DatosIndicadores
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * HU-15 (RF-1504, RF-1524): solo lee Room; la sincronización en tiempo real trae los cambios. La HU-18 solo anota aquí
 * sus tareas diarias (RF-1803), en la tabla de sincronizaciones.
 */
class IndicadoresRepositoryImpl @Inject constructor(
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,
    private val sincronizacionDao: SincronizacionDao,
    @param:IoDispatcher private val io: CoroutineDispatcher
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

    override suspend fun ultimaTareaExitosa(tipo: String): Instant? =
        withContext(io) { sincronizacionDao.observarUltimaExitosa(tipo).first()?.ejecutadaEn }

    override suspend fun registrarTarea(tipo: String, ahora: Instant): Resultado<Unit> = withContext(io) {
        try {
            sincronizacionDao.registrar(SincronizacionEntity(tipoTarea = tipo, ejecutadaEn = ahora, exitosa = true))
            sincronizacionDao.recortar(tipo)
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error al anotar la tarea $tipo", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    private companion object {
        const val TAG = "IndicadoresRepository"
    }
}
