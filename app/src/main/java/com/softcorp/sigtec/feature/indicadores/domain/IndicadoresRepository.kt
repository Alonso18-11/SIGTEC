package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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

    /** HU-18 (RF-1803): la última vez que terminó con éxito la tarea diaria [tipo], o null si nunca. */
    suspend fun ultimaTareaExitosa(tipo: String): Instant?

    /** HU-18 (RF-1803): anota que la tarea diaria [tipo] terminó con éxito, para no repetirla ese día. */
    suspend fun registrarTarea(tipo: String, ahora: Instant): Resultado<Unit>
}
