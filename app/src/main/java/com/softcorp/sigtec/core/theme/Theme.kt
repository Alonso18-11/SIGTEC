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
    background = SuperficieClaro, onBackground = SobreSuperficieClaro,
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
    background = SuperficieOscuro, onBackground = SobreSuperficieOscuro,
    surface = SuperficieOscuro, onSurface = SobreSuperficieOscuro,
    surfaceVariant = VarianteSuperficieOscuro, onSurfaceVariant = SobreVarianteSuperficieOscuro,
    outline = ContornoOscuro, outlineVariant = VarianteContornoOscuro,
    inverseSurface = SuperficieInversaOscuro, inverseOnSurface = SobreSuperficieInversaOscuro,
    inversePrimary = PrimarioInversoOscuro,
    surfaceContainerLowest = ContenedorMasBajoOscuro, surfaceContainerLow = ContenedorBajoOscuro,
    surfaceContainer = ContenedorOscuro, surfaceContainerHigh = ContenedorAltoOscuro,
    surfaceContainerHighest = ContenedorMasAltoOscuro
)

@Composable
fun SigtecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Sin color dinámico: la paleta de SIGTEC se ve igual en todos los celulares.
    val esquema = if (darkTheme) EsquemaOscuro else EsquemaClaro
    val estados = if (darkTheme) EstadosOscuro else EstadosClaro

    CompositionLocalProvider(LocalColoresEstado provides estados) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            content = content
        )
    }
}

// Acceso a lo que Material 3 no trae: SigtecTheme.estados.critico
object SigtecTheme {
    val estados: ColoresEstado
        @Composable @ReadOnlyComposable
        get() = LocalColoresEstado.current
}