package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import java.time.Instant

@Entity(tableName = "sugerencias_ia", indices = [Index("incidenciaId")])
data class SugerenciaIAEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val tipo: TipoSugerencia,
    val contenido: String,
    val modelo: String,
    val generadaEn: Instant,
    val aceptada: Boolean?,
    val decididaPorId: String?,
    val decididaEn: Instant?,
    val pendienteSincronizar: Boolean,
    // HU-14 (RF-1427): la generación o, después, la decisión; la sugerencia no se edita de otra forma
    val actualizadoEn: Instant
)

fun SugerenciaIAEntity.toDomain() = SugerenciaIA(
    id = id, incidenciaId = incidenciaId, tipo = tipo, contenido = contenido, modelo = modelo,
    generadaEn = generadaEn, aceptada = aceptada, decididaPorId = decididaPorId, decididaEn = decididaEn
)

fun SugerenciaIA.toEntity(pendienteSincronizar: Boolean) = SugerenciaIAEntity(
    id = id, incidenciaId = incidenciaId, tipo = tipo, contenido = contenido, modelo = modelo,
    generadaEn = generadaEn, aceptada = aceptada, decididaPorId = decididaPorId, decididaEn = decididaEn,
    pendienteSincronizar = pendienteSincronizar, actualizadoEn = decididaEn ?: generadaEn
)
