package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import java.time.Instant

@Entity(tableName = "movimientos", indices = [Index("incidenciaId")])
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val tipo: TipoMovimiento,
    val autorNombre: String,
    val fecha: Instant,
    val detalle: String?,
    val pendienteSincronizar: Boolean,
    // Firestore lo rechazó de forma permanente (RF-75): deja de reintentarse y espera revisión manual (RF-76)
    val necesitaAtencion: Boolean = false
)

fun MovimientoEntity.toDomain() = Movimiento(id, incidenciaId, tipo, autorNombre, fecha, detalle)

fun Movimiento.toEntity(pendienteSincronizar: Boolean) =
    MovimientoEntity(id, incidenciaId, tipo, autorNombre, fecha, detalle, pendienteSincronizar)