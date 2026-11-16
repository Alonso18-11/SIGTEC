package com.softcorp.sigtec.feature.ia.data

/**
 * Largo máximo de cada texto que se envía a Gemini. La cuota gratuita es por proyecto (RNF de cuota de la spec 014),
 * y una descripción o unas notas no necesitan más.
 */
internal const val MAX_CARACTERES_PEDIDO = 2000

/** HU-14 (RF-1401): instrucciones de sistema para clasificar. Los valores coinciden con `TipoFalla` y `Prioridad`. */
internal val INSTRUCCIONES_CLASIFICACION = """
    Eres el asistente del área de Soporte y Sistemas de una empresa. Clasifica la falla de un equipo de cómputo a
    partir de la descripción que escribió quien la reportó.
    - tipo: HARDWARE (piezas físicas: encendido, fuente, pantalla, teclado, impresora dañada), SOFTWARE (sistema
      operativo, programas, licencias, virus) o RED (internet, cables de red, wifi, carpetas o impresoras compartidas
      que no aparecen en la red).
    - prioridad: ALTA si la persona no puede trabajar o afecta a varias personas; MEDIA si puede trabajar con
      dificultad; BAJA si es una molestia menor.
    Responde solo con el JSON pedido.
""".trimIndent()

/**
 * HU-14 (RF-1401): el pedido de la clasificación lleva solo la descripción del problema; nunca el responsable, su
 * área, el técnico ni el código del equipo (RNF de privacidad).
 */
internal fun pedidoClasificacion(descripcion: String): String =
    "Descripción del problema:\n${descripcion.recortado()}"

internal fun String.recortado(): String = trim().take(MAX_CARACTERES_PEDIDO)
