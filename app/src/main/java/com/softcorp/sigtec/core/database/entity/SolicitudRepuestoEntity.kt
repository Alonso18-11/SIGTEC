package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import java.time.Instant

@Entity(
    tableName = "solicitudes_repuesto",
    indices = [Index("incidenciaId"), Index("solicitadoPorId")]
)
data class SolicitudRepuestoEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val pieza: String,
    val motivo: String,
    val fechaSolicitud: Instant,
    val confirmadoEn: Instant,
    val estadoEntrega: EstadoEntrega,
    val solicitadoPorId: String,
    val solicitadoPorNombre: String,
    val numeroLogistica: String?,
    val stockConsultado: Int?,
    val diasEnvio: Int?,
    val despachadaEn: Instant?,
    val entregadaEn: Instant?,
    val llegadaEstimada: Instant?,
    val rechazadaPorLogistica: Boolean,
    val pendienteSincronizar: Boolean,
    val actualizadoEn: Instant,
    val necesitaAtencion: Boolean = false   // Firestore la rechazó, como el informe técnico (RF-825)
)

fun SolicitudRepuestoEntity.toDomain() = SolicitudRepuesto(
    id = id,
    incidenciaId = incidenciaId,
    pieza = pieza,
    motivo = motivo,
    fechaSolicitud = fechaSolicitud,
    confirmadoEn = confirmadoEn,
    estadoEntrega = estadoEntrega,
    solicitadoPorId = solicitadoPorId,
    solicitadoPorNombre = solicitadoPorNombre,
    numeroLogistica = numeroLogistica,
    stockConsultado = stockConsultado,
    diasEnvio = diasEnvio,
    despachadaEn = despachadaEn,
    entregadaEn = entregadaEn,
    llegadaEstimada = llegadaEstimada,
    rechazadaPorLogistica = rechazadaPorLogistica
)

fun SolicitudRepuesto.toEntity(pendienteSincronizar: Boolean, actualizadoEn: Instant) = SolicitudRepuestoEntity(
    id = id,
    incidenciaId = incidenciaId,
    pieza = pieza,
    motivo = motivo,
    fechaSolicitud = fechaSolicitud,
    confirmadoEn = confirmadoEn,
    estadoEntrega = estadoEntrega,
    solicitadoPorId = solicitadoPorId,
    solicitadoPorNombre = solicitadoPorNombre,
    numeroLogistica = numeroLogistica,
    stockConsultado = stockConsultado,
    diasEnvio = diasEnvio,
    despachadaEn = despachadaEn,
    entregadaEn = entregadaEn,
    llegadaEstimada = llegadaEstimada,
    rechazadaPorLogistica = rechazadaPorLogistica,
    pendienteSincronizar = pendienteSincronizar,
    actualizadoEn = actualizadoEn
)
