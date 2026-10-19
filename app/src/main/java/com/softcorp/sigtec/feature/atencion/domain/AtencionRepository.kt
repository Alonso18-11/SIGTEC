package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface AtencionRepository {
    suspend fun incidencia(id: String): Incidencia?

    fun observarBandeja(tecnicoId: String): Flow<List<Incidencia>>

    suspend fun cerrarConInforme(
        incidencia: Incidencia,
        movimiento: Movimiento,
        informe: InformeTecnico,
        ahora: Instant
    ): Resultado<Unit>
}
