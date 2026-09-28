package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import java.time.Instant

// Sin campo de sincronización: la foto vive solo en este celular (sección 16.1)
@Entity(tableName = "evidencias", indices = [Index("incidenciaId")])
data class EvidenciaEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val tipo: TipoEvidencia,
    val rutaArchivo: String,
    val fechaCaptura: Instant
)

fun EvidenciaEntity.toDomain() = Evidencia(id, incidenciaId, tipo, rutaArchivo, fechaCaptura)

fun Evidencia.toEntity() = EvidenciaEntity(id, incidenciaId, tipo, rutaArchivo, fechaCaptura)