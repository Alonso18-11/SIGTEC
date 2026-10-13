package com.softcorp.sigtec.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Esquinas casi rectas, como en Jira: 3 dp en etiquetas, 4 dp en campos, botones, chips y menús,
// 6 dp en paneles y 8 dp en diálogos. Material 3 usa por defecto de 4 a 28 dp.
internal val Formas = Shapes(
    extraSmall = RoundedCornerShape(3.dp),   // etiquetas de estado y campos de texto
    small = RoundedCornerShape(4.dp),        // botones, chips y menús
    medium = RoundedCornerShape(6.dp),       // paneles y avisos
    large = RoundedCornerShape(8.dp),        // hojas inferiores y FAB
    extraLarge = RoundedCornerShape(8.dp)    // diálogos
)
