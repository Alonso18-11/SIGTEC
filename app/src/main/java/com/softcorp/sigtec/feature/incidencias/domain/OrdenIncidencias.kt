package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Prioridad

/** HU-14 (RF-1433, D-11): «Más recientes» es el orden de la HU-04 y el que aparece al abrir la lista. */
enum class OrdenIncidencias { RECIENTES, URGENTES }

/**
 * HU-14 (RF-1434, RF-1436): con «Más urgentes», primero las pendientes por prioridad (alta, media, baja y al final las
 * que no tienen) y, dentro de cada una, la más antigua primero; después las solucionadas, la más reciente primero.
 * Solo mira la prioridad que eligió el jefe, nunca una sugerencia sin decidir. «Más recientes» deja la lista como
 * llega de Room. Función pura, como `filtrar`.
 */
fun List<Incidencia>.ordenar(orden: OrdenIncidencias): List<Incidencia> = when (orden) {
    OrdenIncidencias.RECIENTES -> this
    OrdenIncidencias.URGENTES -> {
        val (pendientes, solucionadas) = partition { it.estado == EstadoIncidencia.PENDIENTE }
        pendientes.sortedWith(compareBy({ it.prioridad.urgencia() }, { it.fechaRegistro })) +
            solucionadas.sortedByDescending { it.fechaRegistro }
    }
}

private fun Prioridad?.urgencia(): Int = when (this) {
    Prioridad.ALTA -> 0
    Prioridad.MEDIA -> 1
    Prioridad.BAJA -> 2
    null -> 3
}
