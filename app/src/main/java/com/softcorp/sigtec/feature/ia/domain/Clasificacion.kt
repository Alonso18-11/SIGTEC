package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** HU-14 (RF-1401): el tipo de falla y la prioridad que propone Gemini. Es una sugerencia: decide el jefe. */
data class ClasificacionSugerida(val tipo: TipoFalla, val prioridad: Prioridad)

/**
 * Lee la respuesta de Gemini (`{"tipo": "RED", "prioridad": "MEDIA"}`), y también el contenido guardado con
 * [comoContenido]. Devuelve null si no es un objeto JSON o si el tipo o la prioridad no son de los permitidos: así la
 * pantalla no muestra nada y la asignación sigue igual (RF-1406). Acepta mayúsculas o minúsculas y el bloque
 * ```json que a veces agrega el modelo.
 */
fun leerClasificacion(texto: String): ClasificacionSugerida? {
    val objeto = leerObjetoJson(texto) ?: return null
    val tipo = objeto.valorDe("tipo")?.let { valor -> TipoFalla.entries.firstOrNull { it.name.equals(valor, true) } }
    val prioridad = objeto.valorDe("prioridad")?.let { valor -> Prioridad.entries.firstOrNull { it.name.equals(valor, true) } }
    return if (tipo != null && prioridad != null) ClasificacionSugerida(tipo, prioridad) else null
}

/** El JSON canónico que se guarda en `SugerenciaIA.contenido` (P-5 del plan 014). */
fun ClasificacionSugerida.comoContenido(): String = buildJsonObject {
    put("tipo", tipo.name)
    put("prioridad", prioridad.name)
}.toString()

private fun leerObjetoJson(texto: String): JsonObject? {
    val limpio = texto.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return runCatching { Json.parseToJsonElement(limpio) }.getOrNull() as? JsonObject
}

private fun JsonObject.valorDe(clave: String): String? =
    (get(clave) as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()
