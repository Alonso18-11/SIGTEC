package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.Tecnico

@Entity(tableName = "tecnicos")
data class TecnicoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val limiteAtencion: Int,
    val incidenciasActivas: Int
)

fun TecnicoEntity.toDomain() = Tecnico(id, nombre, limiteAtencion, incidenciasActivas)

fun Tecnico.toEntity() = TecnicoEntity(id, nombre, limiteAtencion, incidenciasActivas)