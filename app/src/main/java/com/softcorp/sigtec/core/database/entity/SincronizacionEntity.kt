package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sincronizaciones")
data class SincronizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipoTarea: String,       // "SINCRONIZAR_INCIDENCIAS", "RESUMEN_DIARIO"...
    val ejecutadaEn: Instant,
    val exitosa: Boolean,
    val detalle: String? = null
)