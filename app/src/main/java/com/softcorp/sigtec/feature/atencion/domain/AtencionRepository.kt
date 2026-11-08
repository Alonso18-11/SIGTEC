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

    /** HU-11 (RF-1117): las solicitudes que todavía no tienen número de Logística y no fueron rechazadas. */
    suspend fun solicitudesPorRegistrar(): List<SolicitudRepuesto>

    /** RF-1117: guarda el número que devolvió Logística; la solicitud queda por subir a Firestore. */
    suspend fun marcarRegistrada(solicitudId: String, numero: String, ahora: Instant): Resultado<Unit>

    /** RF-1119: Logística no la acepta; ya no se reintenta. */
    suspend fun marcarRechazada(solicitudId: String, ahora: Instant): Resultado<Unit>
}
