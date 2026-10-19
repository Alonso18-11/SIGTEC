package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class DetalleIncidencia(
    val incidencia: Incidencia,
    val movimientos: List<Movimiento>,    // del más reciente al más antiguo
    val informe: InformeTecnico? = null,  // HU-08: solo si ya está en Room en este celular (RF-827, RF-828)
    val evidencias: List<Evidencia> = emptyList()   // HU-09: las de este celular, de la más antigua a la más reciente
)

/** HU-04: la incidencia con su línea de tiempo; null si no existe en este celular. */
class ObservarDetalleIncidencia @Inject constructor(private val repo: IncidenciaRepository) {
    operator fun invoke(id: String): Flow<DetalleIncidencia?> =
        combine(
            repo.observarPorId(id),
            repo.observarMovimientos(id),
            repo.observarInforme(id),
            repo.observarEvidencias(id)   // RF-936: una foto nueva o eliminada aparece sin salir del detalle
        ) { incidencia, movimientos, informe, evidencias ->
            incidencia?.let {
                DetalleIncidencia(
                    it,
                    movimientos.sortedByDescending(Movimiento::fecha),
                    informe,
                    evidencias.sortedBy(Evidencia::fechaCaptura)   // RF-932
                )
            }
        }
}
