package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.Equipo

@Entity(tableName = "equipos")
data class EquipoEntity(
    @PrimaryKey val codigo: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val oficina: String,
    val responsableNombre: String
)

fun EquipoEntity.toDomain() = Equipo(codigo, tipo, marca, modelo, oficina, responsableNombre)

fun Equipo.toEntity() = EquipoEntity(codigo, tipo, marca, modelo, oficina, responsableNombre)