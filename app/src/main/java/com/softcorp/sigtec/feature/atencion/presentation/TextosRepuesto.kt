package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.feature.atencion.domain.Disponibilidad

// HU-11: los textos de la disponibilidad, tal como los dice la spec (RF-1105 a RF-1108).

/** Singular con 1 día. */
fun textoEnvio(dias: Int): String = if (dias == 1) "envío estimado en 1 día" else "envío estimado en $dias días"

/** Título y detalle de la tarjeta de disponibilidad; `null` mientras se consulta (se muestra el progreso). */
data class TextoDisponibilidad(val titulo: String, val detalle: String?)

fun textoDisponibilidad(disponibilidad: Disponibilidad): TextoDisponibilidad? = when (disponibilidad) {
    Disponibilidad.Consultando -> null
    is Disponibilidad.ConStock -> TextoDisponibilidad(
        titulo = "Disponible en almacén · ${unidades(disponibilidad.unidades)}",
        detalle = "Consultado a Logística hace un momento · ${textoEnvio(disponibilidad.diasEnvio)}"
    )
    is Disponibilidad.SinStock -> TextoDisponibilidad(
        titulo = "Sin stock en almacén · Logística deberá conseguirla",
        detalle = "Consultado a Logística hace un momento"
    )
    Disponibilidad.NoEnCatalogo -> TextoDisponibilidad(
        titulo = "Logística no tiene esa pieza en su catálogo.",
        detalle = "Puedes solicitarla igual y describirla en el motivo."
    )
    Disponibilidad.SinRespuesta -> TextoDisponibilidad(
        titulo = "No se pudo consultar a Logística.",
        detalle = "Puedes solicitarla igual; la disponibilidad se confirmará después."
    )
}

private fun unidades(n: Int) = if (n == 1) "1 unidad" else "$n unidades"
