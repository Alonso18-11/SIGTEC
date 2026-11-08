package com.softcorp.sigtec.core.theme

import androidx.compose.ui.unit.dp

object Espaciado {
    val xs = 4.dp    // entre un ícono y su texto
    val s = 8.dp     // entre elementos relacionados
    val sm = 12.dp   // relleno vertical de filas y entre tarjetas
    val m = 16.dp    // margen de pantalla y padding de tarjetas
    val l = 24.dp    // entre secciones
    val xl = 32.dp   // separaciones grandes
}

val AreaTactilMinima = 48.dp

// Alturas v2 (DESIGN.md, sección 3)
val AltoBoton = 56.dp       // botón principal, secundario y de peligro; el de texto usa AreaTactilMinima
val AltoChip = 40.dp        // chips de filtro, dentro de un área tocable de 48 dp
val AltoEtiqueta = 28.dp    // etiquetas de estado (no se tocan)

/** Solo las tarjetas, el FAB y la barra de acción inferior llevan sombra (DESIGN.md, sección 3). */
object Elevacion {
    val tarjeta = 2.dp
    val flotante = 6.dp
}
