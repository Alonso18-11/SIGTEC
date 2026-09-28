package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow

interface IncidenciaRepository {
    /** Guarda la incidencia y su primer movimiento juntos, pendientes de sincronizar (HU-03). */
    suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit>

    /** Todas, de la más reciente a la más antigua (HU-04). */
    fun observarTodas(): Flow<List<Incidencia>>

    fun observarPorId(id: String): Flow<Incidencia?>

    /** Línea de tiempo de la incidencia, del movimiento más reciente al más antiguo. */
    fun observarMovimientos(incidenciaId: String): Flow<List<Movimiento>>
}
