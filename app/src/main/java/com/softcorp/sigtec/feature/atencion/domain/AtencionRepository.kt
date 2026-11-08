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

    /** HU-11 (RF-1123): las del técnico ya registradas en Logística que todavía no se entregaron. */
    suspend fun solicitudesSinEntregarDe(tecnicoId: String): List<SolicitudRepuesto>

    /** RF-1124: cuántas solicitudes de la incidencia faltan entregar (las por enviar también cuentan). */
    suspend fun contarSinEntregarDe(incidenciaId: String): Int

    /**
     * RF-1123 y RF-1124: guarda el estado que informó Logística y, si con eso llegó el último repuesto, la incidencia
     * de vuelta en «En atención» con su movimiento, todo en una transacción.
     */
    suspend fun aplicarEstado(
        solicitud: SolicitudRepuesto,
        incidencia: Incidencia?,
        movimiento: Movimiento?,
        ahora: Instant
    ): Resultado<Unit>

    /** RF-1123: «Consultado a Logística hoy a las HH:mm» muestra la última consulta correcta. */
    suspend fun registrarConsultaLogistica(ahora: Instant, exitosa: Boolean)

    fun observarUltimaConsultaLogistica(): Flow<Instant?>

    /** HU-11 (RF-1121): las solicitudes que pidió el técnico, con el código de su incidencia y de su equipo. */
    fun observarSolicitudesDe(tecnicoId: String): Flow<List<SolicitudConIncidencia>>
}

/** RF-1122: la solicitud con lo que «Estado de repuestos» muestra de su incidencia; `codigoIncidencia` null = sin número aún. */
data class SolicitudConIncidencia(
    val solicitud: SolicitudRepuesto,
    val codigoIncidencia: String?,
    val codigoEquipo: String
)
