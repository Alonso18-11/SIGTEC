package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.notificaciones.ContenidoAviso

/** HU-18 (RF-1816): los equipos reparados hoy, la misma columna resaltada del tablero (RF-1509, RF-1511). */
val Indicadores.reparadosHoy: Int
    get() = reparadosPorDia.lastOrNull { it.esHoy }?.cantidad ?: 0

/**
 * HU-18 (RF-1813, RF-1814, RF-1820): «Casos demorados» con el número de críticas; null si no hay ninguna, para no
 * avisar. Con el celular bloqueado solo se ve el título.
 */
fun contenidoCasosDemorados(criticas: Int): ContenidoAviso? {
    if (criticas <= 0) return null
    val titulo = "Casos demorados"
    val texto = if (criticas == 1) "1 incidencia lleva más de 5 días pendiente."
    else "$criticas incidencias llevan más de 5 días pendientes."
    return ContenidoAviso(titulo = titulo, texto = texto, textoPublico = titulo)
}

/** HU-18 (RF-1817, RF-1820): «Resumen del día», que se envía aunque no haya reparados. */
fun contenidoResumenDia(reparados: Int, pendientes: Int, criticos: Int): ContenidoAviso {
    val titulo = "Resumen del día"
    val hoy = when (reparados) {
        0 -> "Hoy no se reparó ningún equipo."
        1 -> "Hoy se reparó 1 equipo."
        else -> "Hoy se repararon $reparados equipos."
    }
    val quedan = when (pendientes) {
        0 -> "No quedan pendientes."
        1 -> "Queda 1 pendiente, ${criticosEnTexto(criticos)}."
        else -> "Quedan $pendientes pendientes, ${criticosEnTexto(criticos)}."
    }
    return ContenidoAviso(titulo = titulo, texto = "$hoy $quedan", textoPublico = titulo)
}

private fun criticosEnTexto(criticos: Int) = if (criticos == 1) "1 crítico" else "$criticos críticos"
