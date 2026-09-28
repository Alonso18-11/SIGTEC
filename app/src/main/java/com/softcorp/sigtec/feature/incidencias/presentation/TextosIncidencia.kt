package com.softcorp.sigtec.feature.incidencias.presentation

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** «INC-0124», o «Por sincronizar» mientras la sincronización no le asigne número. */
fun Incidencia.codigoParaMostrar(): String = codigoVisible ?: POR_SINCRONIZAR

const val POR_SINCRONIZAR = "Por sincronizar"

const val SIN_TECNICO = "Sin técnico"

/** Nombre de cada marca en los filtros; coincide con el texto de EtiquetaIncidencia. */
fun MarcaSeguimiento.etiqueta(): String = when (this) {
    MarcaSeguimiento.SIN_ASIGNAR -> "Sin asignar"
    MarcaSeguimiento.ASIGNADA -> "Asignada"
    MarcaSeguimiento.EN_ATENCION -> "En atención"
    MarcaSeguimiento.ESPERANDO_REPUESTO -> "Esperando repuesto"
    MarcaSeguimiento.CERRADA -> "Solucionado"
}

fun TipoFalla.etiqueta(): String = when (this) {
    TipoFalla.HARDWARE -> "Hardware"
    TipoFalla.SOFTWARE -> "Software"
    TipoFalla.RED -> "Red"
}

fun Prioridad.etiqueta(): String = when (this) {
    Prioridad.BAJA -> "Prioridad baja"
    Prioridad.MEDIA -> "Prioridad media"
    Prioridad.ALTA -> "Prioridad alta"
}

/** Título de cada punto de la línea de tiempo del detalle. */
fun TipoMovimiento.titulo(): String = when (this) {
    TipoMovimiento.REGISTRADA -> "Incidencia registrada"
    TipoMovimiento.ASIGNADA -> "Asignada"
    TipoMovimiento.REASIGNADA -> "Reasignada"
    TipoMovimiento.ATENCION_INICIADA -> "Atención iniciada"
    TipoMovimiento.REPUESTO_SOLICITADO -> "Repuesto solicitado"
    TipoMovimiento.REPUESTO_RECIBIDO -> "Repuesto recibido"
    TipoMovimiento.CERRADA -> "Incidencia cerrada"
}

/** Días completos desde que se registró; lo que muestra EtiquetaIncidencia en los casos demorados. */
fun Incidencia.diasPendiente(ahora: Instant): Long =
    ChronoUnit.DAYS.between(fechaRegistro, ahora).coerceAtLeast(0)

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val formatoDia = DateTimeFormatter.ofPattern("dd/MM")
private val formatoCompleto = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")

/** «Hoy 10:42» si es de hoy; si no, «21/09», como en la lista del prototipo. */
fun fechaCorta(fecha: Instant, ahora: Instant, zona: ZoneId = ZoneId.systemDefault()): String {
    val local = fecha.atZone(zona)
    return if (local.toLocalDate() == ahora.atZone(zona).toLocalDate()) "Hoy ${formatoHora.format(local)}"
    else formatoDia.format(local)
}

fun fechaCompleta(fecha: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    formatoCompleto.format(fecha.atZone(zona))

fun horaDe(fecha: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    formatoHora.format(fecha.atZone(zona))
