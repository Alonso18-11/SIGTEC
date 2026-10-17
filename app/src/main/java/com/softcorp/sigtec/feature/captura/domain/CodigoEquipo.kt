package com.softcorp.sigtec.feature.captura.domain

// Guiones que el reconocimiento de texto puede devolver en lugar del guion simple
private val GUIONES = Regex("[‐‑‒–—―−]")
private val ESPACIOS_EN_GUION = Regex("\\s*-\\s*")

/**
 * RF-903: deja el texto leído de la etiqueta listo para buscar el código del equipo:
 * mayúsculas, un solo tipo de guion y sin espacios alrededor de los guiones.
 */
fun normalizarTexto(texto: String): String =
    texto.uppercase()
        .replace(GUIONES, "-")
        .replace(ESPACIOS_EN_GUION, "-")

// Tres partes separadas por guion, sin pegarse a otro código; las letras y los dígitos se corrigen después
private val FORMA_SUELTA = Regex("(?<![A-Z0-9-])([A-Z0-9]{2,4})-([A-Z0-9]{2,5})-([A-Z0-9]{3})(?![A-Z0-9-])")
private val FORMA_CODIGO = Regex("[A-Z]{2,4}-[A-Z]{2,5}-[0-9]{3}")

// Confusiones típicas del reconocimiento entre letras y dígitos
private val COMO_LETRA = mapOf('0' to 'O', '1' to 'I', '5' to 'S', '8' to 'B', '2' to 'Z')
private val COMO_DIGITO = mapOf('O' to '0', 'I' to '1', 'L' to '1', 'S' to '5', 'B' to '8', 'Z' to '2')

private fun String.corregir(tabla: Map<Char, Char>): String = map { tabla[it] ?: it }.joinToString("")

/**
 * RF-903: los códigos de equipo que aparecen en el texto (`PC-CONT-014`), en orden de lectura y sin
 * repetir. Las dos primeras partes son letras y la última, dígitos: según la posición se corrige
 * `0`↔`O`, `1`↔`I`/`L`, `5`↔`S`, `8`↔`B` y `2`↔`Z`.
 */
fun candidatosCodigo(texto: String): List<String> =
    FORMA_SUELTA.findAll(normalizarTexto(texto))
        .map { m ->
            val (tipo, area, numero) = m.destructured
            "${tipo.corregir(COMO_LETRA)}-${area.corregir(COMO_LETRA)}-${numero.corregir(COMO_DIGITO)}"
        }
        .filter { FORMA_CODIGO.matches(it) }
        .distinct()
        .toList()
