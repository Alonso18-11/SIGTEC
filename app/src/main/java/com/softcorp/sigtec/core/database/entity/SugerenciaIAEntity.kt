package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia

@Entity(tableName = "sugerencias_ia", indices = [Index("incidenciaId")])
data class SugerenciaIAEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val tipo: TipoSugerencia,
    val contenido: String,
    val aceptada: Boolean?,
    val pendienteSincronizar: Boolean
)

fun SugerenciaIAEntity.toDomain() = SugerenciaIA(id, incidenciaId, tipo, contenido, aceptada)

fun SugerenciaIA.toEntity(pendienteSincronizar: Boolean) =
    SugerenciaIAEntity(id, incidenciaId, tipo, contenido, aceptada, pendienteSincronizar)