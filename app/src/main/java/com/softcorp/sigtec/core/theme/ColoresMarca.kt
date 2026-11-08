package com.softcorp.sigtec.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores de marca de la v2 (DESIGN.md, sección 3). No son estados de incidencia, por eso viven aparte de
 * `SigtecTheme.estados` (plan del retema, P-4).
 */
@Immutable
data class ColoresMarca(
    val hero: Color,            // fondo de las cabeceras oscuras
    val sobreHero: Color,       // títulos sobre la cabecera
    val sobreHeroTenue: Color,  // subtítulos sobre la cabecera
    val acento: Color,          // solo FAB, micrófono, linterna, máximo del gráfico y avatar propio
    val sobreAcento: Color      // texto e íconos sobre el acento; el acento nunca es color de texto
)

internal val MarcaClaro = ColoresMarca(
    hero = Color(0xFF002E3A),
    sobreHero = Color(0xFFFFFFFF),
    sobreHeroTenue = Color(0xFFB3EBFF),
    acento = Color(0xFFFFB547),
    sobreAcento = Color(0xFF2A1A00)
)

// La cabecera es más clara que el fondo oscuro para no fundirse con él (3,1:1); por eso el subtítulo
// no puede ser el cian del modo claro (plan del retema, sección 7).
internal val MarcaOscuro = ColoresMarca(
    hero = Color(0xFF2C6F80),
    sobreHero = Color(0xFFFFFFFF),
    sobreHeroTenue = Color(0xFFE0F6FC),
    acento = Color(0xFFFFB547),
    sobreAcento = Color(0xFF2A1A00)
)

internal val LocalColoresMarca = staticCompositionLocalOf { MarcaClaro }
