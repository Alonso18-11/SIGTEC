package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Lo que muestra la pantalla de asignación, siempre al día con Room (RF-601 a RF-607). */
data class OpcionesAsignacion(
    val incidencia: Incidencia,
    val oficina: String?,                  // RF-601: null si el equipo no está en el catálogo
    val tecnicos: List<OpcionTecnico>,     // RF-606: vacía si todavía no bajaron
    val bloqueo: ErrorDominio?             // RF-607: por qué no se puede asignar, o null
)

/** HU-06: la incidencia y los técnicos con su carga efectiva; null si la incidencia no está en el celular. */
class ObservarOpcionesAsignacion @Inject constructor(private val repo: AsignacionRepository) {
    operator fun invoke(incidenciaId: String): Flow<OpcionesAsignacion?> =
        combine(
            repo.observarIncidencia(incidenciaId).map { incidencia ->
                incidencia?.let { it to repo.oficinaDeEquipo(it.codigoEquipo) }
            },
            repo.observarTecnicos(),
            repo.observarAsignacionesSinSubir()
        ) { incidenciaYOficina, tecnicos, sinSubir ->
            incidenciaYOficina?.let { (incidencia, oficina) ->
                OpcionesAsignacion(
                    incidencia = incidencia,
                    oficina = oficina,
                    tecnicos = armarOpciones(incidencia, tecnicos, sinSubir),
                    bloqueo = bloqueoParaAsignar(incidencia)
                )
            }
        }
}
