package com.softcorp.sigtec.feature.indicadores.presentation

import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DIA_MES = DateTimeFormatter.ofPattern("dd/MM")
private const val HORAS_DIA = 24.0

/** RF-1502: cuándo se bajaron datos por última vez, relativo a hoy. */
fun textoActualizado(ultima: Instant?, ahora: Instant, zona: ZoneId = ZoneId.systemDefault()): String {
    if (ultima == null) return "Sin datos del servidor todavía"
    val cuando = ultima.atZone(zona)
    val hoy = ahora.atZone(zona).toLocalDate()
    val hora = HORA.format(cuando)
    return when (cuando.toLocalDate()) {
        hoy -> "Actualizado hoy a las $hora"
        hoy.minusDays(1) -> "Actualizado ayer a las $hora"
        else -> "Actualizado el ${DIA_MES.format(cuando)} a las $hora"
    }
}

/** RF-1512, RF-1514: «1,8 días de atención», o en horas si es menos de un día («6 h de atención»). */
fun textoTiempoAtencion(horas: Double): String =
    if (horas < HORAS_DIA) {
        "${horas.roundToInt().coerceAtLeast(1)} h de atención"
    } else {
        // Con coma decimal, como el resto de la app, sin depender de la configuración regional del celular
        val dias = String.format(Locale.ROOT, "%.1f", horas / HORAS_DIA).replace('.', ',')
        "$dias días de atención"
    }

/** RF-1509: la inicial del día bajo cada columna. */
fun diaCorto(dia: LocalDate): String = when (dia.dayOfWeek) {
    DayOfWeek.MONDAY -> "Lun"
    DayOfWeek.TUESDAY -> "Mar"
    DayOfWeek.WEDNESDAY -> "Mié"
    DayOfWeek.THURSDAY -> "Jue"
    DayOfWeek.FRIDAY -> "Vie"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}

/** Para TalkBack (no funcional): el nombre completo del día. */
fun diaLargo(dia: LocalDate): String = when (dia.dayOfWeek) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
}

/** RF-1515: el nombre de cada fila de «Incidencias por estado». */
fun nombreEstado(marca: MarcaSeguimiento): String = when (marca) {
    MarcaSeguimiento.SIN_ASIGNAR -> "Sin asignar"
    MarcaSeguimiento.ASIGNADA -> "Asignadas"
    MarcaSeguimiento.EN_ATENCION -> "En atención"
    MarcaSeguimiento.ESPERANDO_REPUESTO -> "Esperando repuesto"
    MarcaSeguimiento.CERRADA -> "Solucionadas"
}

/** RF-1521: «4 incidencias en 3 meses, evaluar reemplazo». */
fun textoEquipoAEvaluar(incidencias: Int): String = "$incidencias incidencias en 3 meses, evaluar reemplazo"
