package com.softcorp.sigtec.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val EsquemaClaro = lightColorScheme(
    primary = PrimarioClaro, onPrimary = SobrePrimarioClaro,
    primaryContainer = ContenedorPrimarioClaro, onPrimaryContainer = SobreContenedorPrimarioClaro,
    secondary = SecundarioClaro, onSecondary = SobreSecundarioClaro,
    secondaryContainer = ContenedorSecundarioClaro, onSecondaryContainer = SobreContenedorSecundarioClaro,
    tertiary = TerciarioClaro, onTertiary = SobreTerciarioClaro,
    tertiaryContainer = ContenedorTerciarioClaro, onTertiaryContainer = SobreContenedorTerciarioClaro,
    error = ErrorClaro, onError = SobreErrorClaro,
    errorContainer = ContenedorErrorClaro, onErrorContainer = SobreContenedorErrorClaro,
    background = FondoClaro, onBackground = SobreSuperficieClaro,
    surface = SuperficieClaro, onSurface = SobreSuperficieClaro,
    surfaceVariant = VarianteSuperficieClaro, onSurfaceVariant = SobreVarianteSuperficieClaro,
    outline = ContornoClaro, outlineVariant = VarianteContornoClaro,
    inverseSurface = SuperficieInversaClaro, inverseOnSurface = SobreSuperficieInversaClaro,
    inversePrimary = PrimarioInversoClaro,
    surfaceContainerLowest = ContenedorMasBajoClaro, surfaceContainerLow = ContenedorBajoClaro,
    surfaceContainer = ContenedorClaro, surfaceContainerHigh = ContenedorAltoClaro,
    surfaceContainerHighest = ContenedorMasAltoClaro
)

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioOscuro, onPrimary = SobrePrimarioOscuro,
    primaryContainer = ContenedorPrimarioOscuro, onPrimaryContainer = SobreContenedorPrimarioOscuro,
    secondary = SecundarioOscuro, onSecondary = SobreSecundarioOscuro,
    secondaryContainer = ContenedorSecundarioOscuro, onSecondaryContainer = SobreContenedorSecundarioOscuro,
    tertiary = TerciarioOscuro, onTertiary = SobreTerciarioOscuro,
    tertiaryContainer = ContenedorTerciarioOscuro, onTertiaryContainer = SobreContenedorTerciarioOscuro,
    error = ErrorOscuro, onError = SobreErrorOscuro,
    errorContainer = ContenedorErrorOscuro, onErrorContainer = SobreContenedorErrorOscuro,
    background = FondoOscuro, onBackground = SobreSuperficieOscuro,
    surface = SuperficieOscuro, onSurface = SobreSuperficieOscuro,
    surfaceVariant = VarianteSuperficieOscuro, onSurfaceVariant = SobreVarianteSuperficieOscuro,
    outline = ContornoOscuro, outlineVariant = VarianteContornoOscuro,
    inverseSurface = SuperficieInversaOscuro, inverseOnSurface = SobreSuperficieInversaOscuro,
    inversePrimary = PrimarioInversoOscuro,
    surfaceContainerLowest = ContenedorMasBajoOscuro, surfaceContainerLow = ContenedorBajoOscuro,
    surfaceContainer = ContenedorOscuro, surfaceContainerHigh = ContenedorAltoOscuro,
    surfaceContainerHighest = ContenedorMasAltoOscuro
)

/** Tema de SIGTEC. Sin color dinámico: los colores son siempre los de la marca (CLAUDE.md). */
@Composable
fun SigtecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val esquema = if (darkTheme) EsquemaOscuro else EsquemaClaro
    val estados = if (darkTheme) EstadosOscuro else EstadosClaro
    val marca = if (darkTheme) MarcaOscuro else MarcaClaro

    CompositionLocalProvider(LocalColoresEstado provides estados, LocalColoresMarca provides marca) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            shapes = Formas,
            content = content
        )
    }
}

object SigtecTheme {
    val estados: ColoresEstado
        @Composable @ReadOnlyComposable
        get() = LocalColoresEstado.current

    /** hero, acento y sobreAcento (DESIGN.md, sección 3). */
    val marca: ColoresMarca
        @Composable @ReadOnlyComposable
        get() = LocalColoresMarca.current
}
