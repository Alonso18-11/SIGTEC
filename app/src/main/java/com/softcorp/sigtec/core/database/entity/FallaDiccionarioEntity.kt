package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.TipoFalla
import java.time.Instant

@Entity(
    tableName = "fallas_diccionario",
    indices = [Index("componente"), Index("tipo"), Index("incidenciaOrigenId")]
)
data class FallaDiccionarioEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val sintomas: String,
    val componente: String,
    val tipo: TipoFalla,
    val solucion: String,
    val autorId: String,
    val autorNombre: String,
    val incidenciaOrigenId: String?,
    val pendienteSincronizar: Boolean,
    // Las fallas no se editan (D-4 de la spec 013): es la fecha de registro
    val actualizadoEn: Instant
)

fun FallaDiccionarioEntity.toDomain() = FallaDiccionario(
    id = id, titulo = titulo, sintomas = sintomas, componente = componente, tipo = tipo, solucion = solucion,
    autorId = autorId, autorNombre = autorNombre, registradaEn = actualizadoEn, incidenciaOrigenId = incidenciaOrigenId
)

fun FallaDiccionario.toEntity(pendienteSincronizar: Boolean) = FallaDiccionarioEntity(
    id = id, titulo = titulo, sintomas = sintomas, componente = componente, tipo = tipo, solucion = solucion,
    autorId = autorId, autorNombre = autorNombre, incidenciaOrigenId = incidenciaOrigenId,
    pendienteSincronizar = pendienteSincronizar, actualizadoEn = registradaEn
)
