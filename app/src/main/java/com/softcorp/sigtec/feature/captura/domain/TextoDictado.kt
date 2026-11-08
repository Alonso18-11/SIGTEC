package com.softcorp.sigtec.feature.captura.domain

/** HU-10 (RF-1001, D-2): los campos con micrófono. «Piezas cambiadas» no se dicta. */
enum class CampoDictado { DESCRIPCION, DIAGNOSTICO, SOLUCION, OBSERVACIONES }

/**
 * Agrega [nuevo] al final de [actual], separado por un espacio (RF-1008, RF-1011, D-3). No toca lo que ya había,
 * ni siquiera sus espacios: si [actual] ya termina en espacio o salto de línea, no agrega otro.
 */
fun unirTexto(actual: String, nuevo: String): String {
    val agregado = nuevo.trim()
    return when {
        agregado.isEmpty() -> actual
        actual.isBlank() -> agregado
        actual.last().isWhitespace() -> actual + agregado
        else -> "$actual $agregado"
    }
}
