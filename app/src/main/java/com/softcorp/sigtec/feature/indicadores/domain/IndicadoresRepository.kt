package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import kotlinx.coroutines.flow.Flow

/** Lo que el tablero lee de Room (RF-1504). Solo lectura (RF-1524). */
data class DatosIndicadores(
    val incidencias: List<Incidencia>,
    val cierres: List<Movimiento>,
    val tecnicos: List<Tecnico>,
    val asignacionesSinSubir: Map<String, Int>,
    val equipos: List<Equipo>
)

interface IndicadoresRepository {
    fun observarDatos(): Flow<DatosIndicadores>
}
