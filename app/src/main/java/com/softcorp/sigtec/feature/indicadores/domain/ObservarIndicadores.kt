package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.di.DefaultDispatcher
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * HU-15 (RF-1503, RF-1504): solo el jefe (`VER_INDICADORES`). Recalcula cuando cambian los datos de Room o el
 * [reloj] (para que «hoy» y los 7 días se corran solos), fuera del hilo principal.
 */
class ObservarIndicadores @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: IndicadoresRepository,
    @DefaultDispatcher private val calculo: CoroutineDispatcher
) {
    operator fun invoke(reloj: Flow<Instant>, zona: ZoneId = ZoneId.systemDefault()): Resultado<Flow<Indicadores>> =
        when (val r = verificarPermiso(Permiso.VER_INDICADORES)) {
            is Resultado.Error -> r
            is Resultado.Exito -> Resultado.Exito(
                combine(repo.observarDatos(), reloj) { d, ahora ->
                    calcularIndicadores(d.incidencias, d.cierres, d.tecnicos, d.asignacionesSinSubir, d.equipos, ahora, zona)
                }.flowOn(calculo)
            )
        }
}
