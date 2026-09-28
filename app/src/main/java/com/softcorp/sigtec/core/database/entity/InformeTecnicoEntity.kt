package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import java.time.Instant

@Entity(tableName = "informes_tecnicos", indices = [Index("incidenciaId", unique = true)])
data class InformeTecnicoEntity(
    @PrimaryKey val id: String,
    val incidenciaId: String,
    val trabajoRealizado: String,
    val piezasCambiadas: String?,
    val observaciones: String?,
    val fechaCierre: Instant,
    val confirmadoEn: Instant,
    val pendienteSincronizar: Boolean
)

fun InformeTecnicoEntity.toDomain() = InformeTecnico(
    id, incidenciaId, trabajoRealizado, piezasCambiadas, observaciones, fechaCierre, confirmadoEn
)

fun InformeTecnico.toEntity(pendienteSincronizar: Boolean) = InformeTecnicoEntity(
    id, incidenciaId, trabajoRealizado, piezasCambiadas, observaciones,
    fechaCierre, confirmadoEn, pendienteSincronizar
)