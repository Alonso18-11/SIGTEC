package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
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

    /** HU-11 (RF-1113, RF-1115): la incidencia con su nueva marca, el movimiento y la solicitud, en una transacción. */
    suspend fun solicitarRepuesto(
        incidencia: Incidencia,
        movimiento: Movimiento,
        solicitud: SolicitudRepuesto,
        ahora: Instant
    ): Resultado<Unit>
}
