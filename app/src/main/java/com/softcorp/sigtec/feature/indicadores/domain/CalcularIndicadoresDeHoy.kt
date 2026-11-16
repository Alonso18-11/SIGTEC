package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.di.DefaultDispatcher
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * HU-18 (RF-1812, RF-1816): los indicadores del área en este momento, para los avisos del jefe. Es el mismo cálculo del
 * tablero (HU-15) con lo que hay en Room, así los avisos y el tablero dicen lo mismo. Solo el jefe (`VER_INDICADORES`).
 */
class CalcularIndicadoresDeHoy @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: IndicadoresRepository,
    @param:DefaultDispatcher private val calculo: CoroutineDispatcher
) {
    suspend operator fun invoke(ahora: Instant, zona: ZoneId): Resultado<Indicadores> {
        when (val r = verificarPermiso(Permiso.VER_INDICADORES)) {
            is Resultado.Error -> return r
            is Resultado.Exito -> Unit
        }
        val d = repo.observarDatos().first()
        return Resultado.Exito(
            withContext(calculo) {
                calcularIndicadores(d.incidencias, d.cierres, d.tecnicos, d.asignacionesSinSubir, d.equipos, ahora, zona)
            }
        )
    }
}
