package com.softcorp.sigtec.feature.captura.presentation

/**
 * Qué dibuja una pantalla que usa un recurso del celular (la cámara o el micrófono) según su permiso de Android
 * (HU-09: RF-916 a RF-920, RF-922; HU-10: RF-1019 a RF-1023).
 */
enum class VistaPermiso { SIN_RECURSO, EXPLICAR, NEGADO, IR_A_AJUSTES, CONCEDIDO }

/**
 * Traduce lo que responde Android a la vista que corresponde. Es pura para poder probarla sin celular.
 * [androidLoExplicaria] es lo que devuelve `shouldShowRequestPermissionRationale`: verdadero solo después de
 * una negativa en la que Android todavía mostraría su diálogo.
 */
fun vistaPermiso(
    tieneRecurso: Boolean,
    concedido: Boolean,
    yaSePidio: Boolean,
    androidLoExplicaria: Boolean
): VistaPermiso = when {
    !tieneRecurso -> VistaPermiso.SIN_RECURSO // RF-920, RF-1017
    concedido -> VistaPermiso.CONCEDIDO // RF-917, RF-1020
    androidLoExplicaria -> VistaPermiso.NEGADO // RF-918, RF-1021
    !yaSePidio -> VistaPermiso.EXPLICAR // RF-916, RF-922, RF-1019: primero se explica para qué
    else -> VistaPermiso.IR_A_AJUSTES // RF-919, RF-1022: Android ya no muestra su diálogo
}
