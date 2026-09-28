package com.softcorp.sigtec.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.softcorp.sigtec.core.domain.model.*
import java.time.Instant

@Entity(
    tableName = "incidencias",
    indices = [Index("tecnicoAsignadoId"), Index("codigoEquipo"), Index("pendienteSincronizar")]
)
data class IncidenciaEntity(
    @PrimaryKey val id: String,
    val numero: Int?,
    val codigoEquipo: String,
    val descripcion: String,
    val usuarioResponsable: String,
    val fechaRegistro: Instant,
    val registradoPorId: String,
    val registradoPorNombre: String,
    val marca: MarcaSeguimiento,
    val tipoFalla: TipoFalla?,
    val prioridad: Prioridad?,
    val tecnicoAsignadoId: String?,
    val tecnicoAsignadoNombre: String?,
    val critica: Boolean,
    // Metadatos de sincronización: existen solo aquí, el dominio no los conoce
    val pendienteSincronizar: Boolean,
    val actualizadoEn: Instant
)

fun IncidenciaEntity.toDomain() = Incidencia(
    id = id, numero = numero, codigoEquipo = codigoEquipo, descripcion = descripcion,
    usuarioResponsable = usuarioResponsable, fechaRegistro = fechaRegistro,
    registradoPorId = registradoPorId, registradoPorNombre = registradoPorNombre,
    marca = marca, tipoFalla = tipoFalla, prioridad = prioridad,
    tecnicoAsignadoId = tecnicoAsignadoId, tecnicoAsignadoNombre = tecnicoAsignadoNombre,
    critica = critica
)

fun Incidencia.toEntity(pendienteSincronizar: Boolean, actualizadoEn: Instant = Instant.now()) =
    IncidenciaEntity(
        id = id, numero = numero, codigoEquipo = codigoEquipo, descripcion = descripcion,
        usuarioResponsable = usuarioResponsable, fechaRegistro = fechaRegistro,
        registradoPorId = registradoPorId, registradoPorNombre = registradoPorNombre,
        marca = marca, tipoFalla = tipoFalla, prioridad = prioridad,
        tecnicoAsignadoId = tecnicoAsignadoId, tecnicoAsignadoNombre = tecnicoAsignadoNombre,
        critica = critica, pendienteSincronizar = pendienteSincronizar, actualizadoEn = actualizadoEn
    )