package com.softcorp.sigtec.feature.asignacion.presentation

/** RF-601: «Luis Ramírez» → «LR», para el avatar de la tarjeta del técnico. */
fun iniciales(nombre: String): String =
    nombre.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

/** RF-605: «Asignar a Luis Ramírez», o «Reasignar a …» si la incidencia ya tenía técnico. */
fun textoBotonAsignar(nombreElegido: String?, esReasignacion: Boolean): String {
    val accion = if (esReasignacion) "Reasignar" else "Asignar"
    return if (nombreElegido == null) accion else "$accion a $nombreElegido"
}

/** RF-605: sin prometer el aviso, que es de la HU-07. */
fun textoBandeja(nombreElegido: String): String = "La incidencia aparecerá en la bandeja de $nombreElegido"
