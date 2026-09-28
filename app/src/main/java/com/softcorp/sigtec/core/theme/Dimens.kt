package com.softcorp.sigtec.core.theme

import androidx.compose.ui.unit.dp

// Retícula de 8 puntos (sección 12 del documento)
object Espaciado {
    val xs = 4.dp    // entre un ícono y su texto
    val s = 8.dp     // entre elementos relacionados
    val m = 16.dp    // margen de pantalla y padding de tarjetas
    val l = 24.dp    // entre secciones
    val xl = 32.dp   // separaciones grandes
}

// Los componentes de Material 3 ya garantizan este mínimo;
// úsalo solo en elementos tocables hechos a mano.
val AreaTactilMinima = 48.dp