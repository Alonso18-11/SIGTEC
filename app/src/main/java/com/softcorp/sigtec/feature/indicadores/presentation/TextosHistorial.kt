package com.softcorp.sigtec.feature.indicadores.presentation

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import com.softcorp.sigtec.feature.indicadores.domain.AtencionEquipo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private const val HORAS_DIA = 24.0

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

/** Número y etiqueta de una `TarjetaMetrica`. */
data class TextoMetrica(val valor: String, val etiqueta: String)

/**
 * RF-1612 (D-3): el promedio en días con un decimal y coma («2,1»), o en horas si es menos de un día, siempre con «en 3
 * meses» en la etiqueta. Sin promedio, «-».
 */
fun metricaPromedio(horas: Double?): TextoMetrica = when {
    horas == null -> TextoMetrica("-", "Días promedio en 3 meses")
    horas < HORAS_DIA -> TextoMetrica("${horas.roundToInt().coerceAtLeast(1)}", "Horas promedio en 3 meses")
    // Con coma decimal, como el tablero, sin depender de la configuración regional del celular
    else -> TextoMetrica(String.format(Locale.ROOT, "%.1f", horas / HORAS_DIA).replace('.', ','), "Días promedio en 3 meses")
}

/** RF-1607: las dos líneas del catálogo, con coma en lugar de «·» (retema v2). */
fun lineasEquipo(equipo: Equipo): List<String> = listOf(
    listOf(equipo.tipo, "${equipo.marca} ${equipo.modelo}".trim()).filter { it.isNotBlank() }.joinToString(", "),
    listOf(equipo.oficina, "responsable ${equipo.responsableNombre}".takeIf { equipo.responsableNombre.isNotBlank() })
        .filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
).filter { it.isNotBlank() }

/** RF-1616: «27/09/2026, INC-0124», o «Por sincronizar» si aún no tiene número. */
fun encabezadoAtencion(atencion: AtencionEquipo, zona: ZoneId = ZoneId.systemDefault()): String =
    "${FECHA.format(atencion.fechaRegistro.atZone(zona))}, ${atencion.codigoVisible ?: POR_SINCRONIZAR}"

/** RF-1617, RF-1618: lo que se hizo, solo en las cerradas; si su informe no llegó, se avisa. */
fun trabajoAtencion(atencion: AtencionEquipo): String? = when {
    !atencion.cerrada -> null
    atencion.trabajoRealizado != null -> atencion.trabajoRealizado
    else -> "Informe por descargar"
}

/** RF-1617: quien la atendió o la tiene asignada. */
fun tecnicoAtencion(atencion: AtencionEquipo): String = atencion.tecnicoNombre ?: "Sin técnico"

/** El estado como se lee en voz alta (la etiqueta visible es `EtiquetaIncidencia`). */
private fun estadoParaLeer(atencion: AtencionEquipo): String {
    val marca = when (atencion.marca) {
        MarcaSeguimiento.SIN_ASIGNAR -> "Sin asignar"
        MarcaSeguimiento.ASIGNADA -> "Asignada"
        MarcaSeguimiento.EN_ATENCION -> "En atención"
        MarcaSeguimiento.ESPERANDO_REPUESTO -> "Esperando repuesto"
        MarcaSeguimiento.CERRADA -> "Solucionado"
    }
    return if (atencion.critica) "Crítica, $marca" else marca
}

/** Accesibilidad (RNF): cada atención se lee como una sola frase, con la fecha completa. */
fun descripcionAtencion(atencion: AtencionEquipo, zona: ZoneId = ZoneId.systemDefault()): String {
    val fecha = atencion.fechaRegistro.atZone(zona)
    return listOfNotNull(
        "${fecha.dayOfMonth} de ${MESES[fecha.monthValue - 1]} de ${fecha.year}",
        atencion.codigoVisible ?: POR_SINCRONIZAR,
        estadoParaLeer(atencion),
        atencion.descripcion,
        trabajoAtencion(atencion),
        tecnicoAtencion(atencion)
    ).joinToString(", ")
}

/** Días desde el registro, para la etiqueta de «Esperando repuesto» y de crítica, como la lista (HU-04). */
fun diasDesdeRegistro(atencion: AtencionEquipo, ahora: Instant): Long =
    ChronoUnit.DAYS.between(atencion.fechaRegistro, ahora).coerceAtLeast(0)
