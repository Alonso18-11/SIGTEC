package com.softcorp.sigtec.feature.ia.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Largo máximo de cada campo del borrador antes de mostrarlo (RNF de respuestas de la spec 014). */
const val MAX_CARACTERES_CAMPO = 1000

/** HU-14 (RF-1419): el borrador del informe técnico que redacta Gemini. El técnico lo revisa antes de cerrar. */
data class BorradorInforme(
    val diagnostico: String,
    val trabajoRealizado: String,
    val piezasCambiadas: String,
    val observaciones: String
)

/**
 * Lo que se envía para redactar el borrador (RF-1417): solo el texto del problema, las notas del técnico y lo que ya
 * escribió en el informe. Nunca nombres, el responsable, el área, el técnico ni el código del equipo.
 */
data class EntradaBorrador(
    val descripcion: String,
    val notas: String,
    val diagnostico: String = "",
    val trabajoRealizado: String = "",
    val piezasCambiadas: String = "",
    val observaciones: String = ""
)

/**
 * Lee la respuesta de Gemini, y también el contenido guardado con [comoContenido]. Cada campo se recorta y se deja en
 * [MAX_CARACTERES_CAMPO]. Devuelve null si no es un objeto JSON o si quedan vacíos el diagnóstico o el trabajo
 * realizado (RF-1419, RF-1423); las piezas y las observaciones pueden venir vacías.
 */
fun leerBorrador(texto: String): BorradorInforme? {
    val objeto = leerObjetoJson(texto) ?: return null
    val borrador = BorradorInforme(
        diagnostico = objeto.campo("diagnostico"),
        trabajoRealizado = objeto.campo("trabajoRealizado"),
        piezasCambiadas = objeto.campo("piezasCambiadas"),
        observaciones = objeto.campo("observaciones")
    )
    return borrador.takeIf { it.diagnostico.isNotEmpty() && it.trabajoRealizado.isNotEmpty() }
}

/** El JSON canónico que se guarda en `SugerenciaIA.contenido` (P-5 del plan 014). */
fun BorradorInforme.comoContenido(): String = buildJsonObject {
    put("diagnostico", diagnostico)
    put("trabajoRealizado", trabajoRealizado)
    put("piezasCambiadas", piezasCambiadas)
    put("observaciones", observaciones)
}.toString()

private fun leerObjetoJson(texto: String): JsonObject? {
    val limpio = texto.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return runCatching { Json.parseToJsonElement(limpio) }.getOrNull() as? JsonObject
}

private fun JsonObject.campo(clave: String): String =
    (get(clave) as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.take(MAX_CARACTERES_CAMPO).orEmpty()
