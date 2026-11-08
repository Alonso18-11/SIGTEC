package com.softcorp.sigtec.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Esquinas v2 (DESIGN.md, sección 3). Cada medida va al tamaño de Material 3 que ya usan sus componentes:
// OutlinedTextField y los menús toman extraSmall, Card toma medium y los diálogos extraLarge
// (plan del retema, 2.7).
internal val Formas = Shapes(
    extraSmall = RoundedCornerShape(16.dp),   // campos de texto y menús
    small = RoundedCornerShape(16.dp),        // paneles y avisos pequeños
    medium = RoundedCornerShape(20.dp),       // tarjetas de lista
    large = RoundedCornerShape(24.dp),        // tarjetas destacadas
    extraLarge = RoundedCornerShape(32.dp)    // diálogos
)

/** Botones, chips, etiquetas de estado y FAB: forma de píldora. */
val FormaCompleta = RoundedCornerShape(percent = 50)

/** Cabecera oscura: solo las esquinas inferiores van redondeadas. */
val FormaCabecera = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

/** Hoja inferior y barra de acción pegada abajo: solo las esquinas superiores. */
val FormaHoja = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
