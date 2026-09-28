package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento

interface IncidenciaRepository {
    /** Guarda la incidencia y su primer movimiento juntos, pendientes de sincronizar (HU-03). */
    suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit>
}
