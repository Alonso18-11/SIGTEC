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
