package com.softcorp.sigtec.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.softcorp.sigtec.R

// Inter (licencia OFL, docs/licencias/Inter-OFL.txt): la de Linear, GitLab y muchas herramientas de
// trabajo. Se empaqueta en res/font, así que no depende de Google Fonts ni de una librería extra.
internal val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun estilo(tamano: Int, alto: Int, peso: FontWeight, espaciado: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = espaciado.sp
)

// Escala más compacta que la de Material 3, como en Jira: títulos en SemiBold y sin espaciado extra
// entre letras (Inter ya viene ajustada). Usos principales:
//   titleLarge  = 20 sp -> títulos de pantalla
//   titleMedium = 16 sp -> título de una fila o de una sección
//   bodyLarge   = 16 sp -> cuerpo del texto
//   bodyMedium  = 14 sp -> datos secundarios de una fila
//   labelSmall  = 11 sp -> etiquetas de estado y encabezados de sección (en mayúsculas)
internal val Tipografia = Typography(
    displayLarge = estilo(57, 64, FontWeight.Normal, -0.25),
    displayMedium = estilo(45, 52, FontWeight.Normal),
    displaySmall = estilo(36, 44, FontWeight.Normal),
    headlineLarge = estilo(32, 40, FontWeight.SemiBold, -0.2),
    headlineMedium = estilo(28, 36, FontWeight.SemiBold, -0.2),
    headlineSmall = estilo(24, 32, FontWeight.SemiBold, -0.1),
    titleLarge = estilo(20, 28, FontWeight.SemiBold, -0.1),
    titleMedium = estilo(16, 24, FontWeight.SemiBold),
    titleSmall = estilo(14, 20, FontWeight.SemiBold),
    bodyLarge = estilo(16, 24, FontWeight.Normal),
    bodyMedium = estilo(14, 20, FontWeight.Normal),
    bodySmall = estilo(12, 16, FontWeight.Normal),
    labelLarge = estilo(14, 20, FontWeight.Medium),
    labelMedium = estilo(12, 16, FontWeight.Medium),
    labelSmall = estilo(11, 16, FontWeight.Bold, 0.3)
)
