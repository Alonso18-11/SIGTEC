package com.softcorp.sigtec.feature.asignacion.presentation

import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.feature.ia.domain.ClasificacionSugerida
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta

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

/** HU-14 (RF-1408): las opciones del tipo en «Clasificación»; null es «Sin clasificar». */
fun textoTipoElegido(tipo: TipoFalla?): String = tipo?.etiqueta() ?: "Sin clasificar"

/** HU-14 (RF-1408): las opciones de la prioridad, cortas para que quepan en las pestañas. */
fun textoPrioridadElegida(prioridad: Prioridad?): String = when (prioridad) {
    null -> "Sin prioridad"
    Prioridad.BAJA -> "Baja"
    Prioridad.MEDIA -> "Media"
    Prioridad.ALTA -> "Alta"
}

/** HU-14 (RF-1404): «Red, prioridad media». */
fun textoClasificacionSugerida(sugerida: ClasificacionSugerida): String =
    "${sugerida.tipo.etiqueta()}, ${sugerida.prioridad.etiqueta().lowercase()}"

/** HU-14 (RF-1404): la marca de lo generado, debajo de la sugerencia. */
const val NOTA_SUGERENCIA = "Generada automáticamente. Tú decides la clasificación."
