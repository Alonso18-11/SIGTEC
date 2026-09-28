package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.TipoFalla

@Entity(tableName = "fallas_diccionario", indices = [Index("componente"), Index("tipo")])
data class FallaDiccionarioEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val sintomas: String,
    val componente: String,
    val tipo: TipoFalla,
    val solucion: String,
    val autorNombre: String,
    val pendienteSincronizar: Boolean
)

fun FallaDiccionarioEntity.toDomain() =
    FallaDiccionario(id, titulo, sintomas, componente, tipo, solucion, autorNombre)

fun FallaDiccionario.toEntity(pendienteSincronizar: Boolean) =
    FallaDiccionarioEntity(id, titulo, sintomas, componente, tipo, solucion, autorNombre, pendienteSincronizar)