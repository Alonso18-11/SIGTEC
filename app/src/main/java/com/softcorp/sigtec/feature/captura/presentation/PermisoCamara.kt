package com.softcorp.sigtec.feature.captura.presentation

/** Qué dibuja una pantalla con cámara según el permiso de Android (RF-916 a RF-920, RF-922). */
enum class VistaPermiso { SIN_CAMARA, EXPLICAR, NEGADO, IR_A_AJUSTES, CAMARA }

/**
 * Traduce lo que responde Android a la vista que corresponde. Es pura para poder probarla sin celular.
 * [androidLoExplicaria] es lo que devuelve `shouldShowRequestPermissionRationale`: verdadero solo después de
 * una negativa en la que Android todavía mostraría su diálogo.
 */
fun vistaPermiso(
    tieneCamara: Boolean,
    concedido: Boolean,
    yaSePidio: Boolean,
    androidLoExplicaria: Boolean
): VistaPermiso = when {
    !tieneCamara -> VistaPermiso.SIN_CAMARA // RF-920
    concedido -> VistaPermiso.CAMARA // RF-917
    androidLoExplicaria -> VistaPermiso.NEGADO // RF-918
    !yaSePidio -> VistaPermiso.EXPLICAR // RF-916 y RF-922: primero se explica para qué
    else -> VistaPermiso.IR_A_AJUSTES // RF-919: Android ya no muestra su diálogo
}
