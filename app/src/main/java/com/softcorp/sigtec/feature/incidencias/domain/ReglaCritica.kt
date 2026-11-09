package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import java.time.Duration
import java.time.Instant

/** Días que una incidencia puede seguir pendiente antes de contar como crítica (HU-15, D-1). */
const val DIAS_PARA_CRITICA = 5L

/**
 * HU-15 (RF-1508, D-1): crítica = pendiente hace más de 5 días, o marcada con `critica` (lo hará la HU-18). Una sola
 * regla para Inicio y el tablero, así sus números coinciden.
 */
fun esCritica(incidencia: Incidencia, ahora: Instant): Boolean =
    incidencia.estado == EstadoIncidencia.PENDIENTE &&
        (incidencia.critica || incidencia.fechaRegistro.isBefore(ahora.minus(Duration.ofDays(DIAS_PARA_CRITICA))))
