package com.softcorp.sigtec.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.softcorp.sigtec.R

// Inter (licencia OFL, docs/licencias/Inter-OFL.txt), empaquetada en res/font: no depende de Google Fonts
// ni de una librería extra.
internal val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun estilo(tamano: Float, alto: Float, peso: FontWeight, espaciado: Float = 0f) = TextStyle(
    fontFamily = Inter,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = espaciado.sp
)

// Escala v2 (DESIGN.md, sección 3). Donde DESIGN.md dice peso 800 se usa Bold (700), porque res/font
// trae Inter de 400 a 700 (plan del retema, P-3). Alto de línea: unas 1,3 veces el tamaño en títulos y
// 1,45 en el cuerpo. Usos principales:
//   headlineLarge  = 28 sp -> título de una pantalla de lista
//   headlineMedium = 24 sp -> título dentro de la cabecera oscura
//   titleLarge     = 22 sp -> barra superior de formularios
//   titleMedium    = 17 sp -> título de sección
//   titleSmall     = 15,5 sp -> título de tarjeta
//   bodyLarge      = 16 sp -> texto de campos
//   bodyMedium     = 14,5 sp -> texto de tarjeta
//   bodySmall      = 13 sp -> datos secundarios
//   labelLarge     = 16 sp -> texto de los botones (Material 3 lo toma de aquí)
//   labelMedium    = 12,5 sp -> chips, etiquetas y códigos de incidencia
internal val Tipografia = Typography(
    displayLarge = estilo(57f, 64f, FontWeight.Normal, -0.25f),
    displayMedium = estilo(45f, 52f, FontWeight.Normal),
    displaySmall = estilo(36f, 44f, FontWeight.Normal),
    headlineLarge = estilo(28f, 36f, FontWeight.Bold, -0.3f),
    headlineMedium = estilo(24f, 31f, FontWeight.Bold, -0.2f),
    headlineSmall = estilo(22f, 29f, FontWeight.Bold, -0.1f),
    titleLarge = estilo(22f, 29f, FontWeight.Bold, -0.1f),
    titleMedium = estilo(17f, 22f, FontWeight.Bold),
    titleSmall = estilo(15.5f, 20f, FontWeight.Bold),
    bodyLarge = estilo(16f, 23f, FontWeight.Normal),
    bodyMedium = estilo(14.5f, 21f, FontWeight.Normal),
    bodySmall = estilo(13f, 19f, FontWeight.Medium),
    labelLarge = estilo(16f, 20f, FontWeight.Bold),
    labelMedium = estilo(12.5f, 16f, FontWeight.Bold),
    labelSmall = estilo(11f, 16f, FontWeight.SemiBold, 0.2f)
)
