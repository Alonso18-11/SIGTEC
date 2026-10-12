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
    val diagnostico: String,
    val trabajoRealizado: String,
    val piezasCambiadas: String?,
    val observaciones: String?,
    val fechaCierre: Instant,
    val confirmadoEn: Instant,
    val pendienteSincronizar: Boolean,
    val necesitaAtencion: Boolean = false // RF-825: el padre (incidencia) se rechazó al sincronizar
)

fun InformeTecnicoEntity.toDomain() = InformeTecnico(
    id = id,
    incidenciaId = incidenciaId,
    diagnostico = diagnostico,
    trabajoRealizado = trabajoRealizado,
    piezasCambiadas = piezasCambiadas,
    observaciones = observaciones,
    fechaCierre = fechaCierre,
    confirmadoEn = confirmadoEn
)

fun InformeTecnico.toEntity(pendienteSincronizar: Boolean) = InformeTecnicoEntity(
    id = id,
    incidenciaId = incidenciaId,
    diagnostico = diagnostico,
    trabajoRealizado = trabajoRealizado,
    piezasCambiadas = piezasCambiadas,
    observaciones = observaciones,
    fechaCierre = fechaCierre,
    confirmadoEn = confirmadoEn,
    pendienteSincronizar = pendienteSincronizar
)