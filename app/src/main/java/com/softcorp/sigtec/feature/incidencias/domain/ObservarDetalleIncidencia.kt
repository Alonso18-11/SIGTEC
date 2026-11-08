package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DetalleIncidencia(
    val incidencia: Incidencia,
    val movimientos: List<Movimiento>,    // del más reciente al más antiguo
    val informe: InformeTecnico? = null,  // HU-08: solo si ya está en Room en este celular (RF-827, RF-828)
    val evidencias: List<Evidencia> = emptyList(),  // HU-09: las de este celular, de la más antigua a la más reciente
    val solicitudes: List<SolicitudRepuesto> = emptyList()   // HU-11 (RF-1120): de la más reciente a la más antigua
)

/** HU-04: la incidencia con su línea de tiempo; null si no existe en este celular. */
class ObservarDetalleIncidencia @Inject constructor(private val repo: IncidenciaRepository) {
    operator fun invoke(id: String): Flow<DetalleIncidencia?> =
        combine(
            repo.observarPorId(id),
            repo.observarMovimientos(id),
            repo.observarInforme(id),
            repo.observarEvidencias(id),  // RF-936: una foto nueva o eliminada aparece sin salir del detalle
            repo.observarSolicitudes(id)  // HU-11 (RF-1120): también las que llegan de otro celular
        ) { incidencia, movimientos, informe, evidencias, solicitudes ->
            incidencia?.let {
                DetalleIncidencia(
                    it,
                    movimientos.sortedByDescending(Movimiento::fecha),
                    informe,
                    evidencias.sortedBy(Evidencia::fechaCaptura),   // RF-932
                    solicitudes.sortedByDescending(SolicitudRepuesto::fechaSolicitud)
                )
            }
        }
}
