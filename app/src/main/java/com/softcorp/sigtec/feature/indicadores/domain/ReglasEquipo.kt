package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import java.time.Duration
import java.time.Instant

/** HU-15 (RF-1521) y HU-16 (D-2): la ventana de «3 meses» que miran el tablero y el historial del equipo. */
internal const val DIAS_EQUIPO = 90L

/** Desde cuántas incidencias en esa ventana un equipo es crítico y conviene evaluar su reemplazo. */
internal const val MINIMO_EQUIPO = 3

/** La incidencia se registró en los últimos 90 días, contando hoy. */
fun registradaEnTresMeses(incidencia: Incidencia, ahora: Instant): Boolean =
    !incidencia.fechaRegistro.isBefore(ahora.minus(Duration.ofDays(DIAS_EQUIPO)))

/** D-2: la misma regla en el tablero (equipo a evaluar) y en el historial (etiqueta «Equipo crítico»). */
fun esEquipoCritico(incidenciasEnTresMeses: Int): Boolean = incidenciasEnTresMeses >= MINIMO_EQUIPO
