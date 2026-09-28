package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import java.time.Instant

@Entity(tableName = "solicitudes_repuesto", indices = [Index("incidenciaId")])
data class SolicitudRepuestoEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val pieza: String,
    val motivo: String,
    val fechaSolicitud: Instant,
    val confirmadoEn: Instant,
    val estadoEntrega: EstadoEntrega,
    val pendienteSincronizar: Boolean
)

fun SolicitudRepuestoEntity.toDomain() = SolicitudRepuesto(
    id, incidenciaId, pieza, motivo, fechaSolicitud, confirmadoEn, estadoEntrega
)

fun SolicitudRepuesto.toEntity(pendienteSincronizar: Boolean) = SolicitudRepuestoEntity(
    id, incidenciaId, pieza, motivo, fechaSolicitud, confirmadoEn, estadoEntrega, pendienteSincronizar
)