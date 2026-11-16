package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow

/** Lo que el historial de un equipo lee de Room (RF-1622). Solo lectura (RF-1626). */
data class DatosHistorial(
    val equipo: Equipo?,
    val incidencias: List<Incidencia>,
    val cierres: List<Movimiento>,
    val informes: List<InformeTecnico>
)

interface HistorialRepository {
    fun observarDatos(codigo: String): Flow<DatosHistorial>
}
