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

/** HU-14 (RF-1417): instrucciones de sistema para el borrador. Las claves coinciden con `BorradorInforme`. */
internal val INSTRUCCIONES_BORRADOR = """
    Eres el asistente del área de Soporte y Sistemas de una empresa. Redacta en español el informe técnico de cierre
    de una incidencia a partir de las notas del técnico. Escribe en tercera persona y en pasado, con frases cortas.
    - diagnostico: la causa del problema.
    - trabajoRealizado: lo que se hizo para resolverlo.
    - piezasCambiadas: solo las piezas que las notas dicen que se cambiaron.
    - observaciones: recomendaciones o pendientes que aparezcan en las notas.
    No inventes piezas, causas, nombres ni datos que no estén en lo enviado; si algo no aparece, deja ese campo vacío.
    Si el técnico ya escribió un campo, respétalo y solo mejora su redacción.
    Responde solo con el JSON pedido.
""".trimIndent()

/**
 * HU-14 (RF-1417): el pedido del borrador lleva la descripción, las notas y solo los campos del informe que el técnico
 * ya escribió, cada uno con su nombre. Nada más (RNF de privacidad).
 */
internal fun pedidoBorrador(
    descripcion: String,
    notas: String,
    diagnostico: String = "",
    trabajoRealizado: String = "",
    piezasCambiadas: String = "",
    observaciones: String = ""
): String = buildString {
    append("Descripción del problema:\n").append(descripcion.recortado())
    append("\n\nNotas del técnico:\n").append(notas.recortado())
    listOf(
        "Diagnóstico" to diagnostico,
        "Trabajo realizado" to trabajoRealizado,
        "Piezas cambiadas" to piezasCambiadas,
        "Observaciones" to observaciones
    ).filter { it.second.isNotBlank() }.forEach { (nombre, valor) ->
        append("\n\nLo que el técnico ya escribió en «").append(nombre).append("»:\n").append(valor.recortado())
    }
}

internal fun String.recortado(): String = trim().take(MAX_CARACTERES_PEDIDO)
