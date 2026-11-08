package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import java.util.Locale

/**
 * Círculo con las iniciales de una persona (DESIGN.md, sección 4). El usuario de la sesión va en `acento`, para
 * reconocerse de un vistazo en las listas; los demás, en `secondaryContainer`.
 */
@Composable
fun Avatar(
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 32.dp,
    esUsuarioActual: Boolean = false
) {
    val fondo = if (esUsuarioActual) SigtecTheme.marca.acento else MaterialTheme.colorScheme.secondaryContainer
    val texto = if (esUsuarioActual) SigtecTheme.marca.sobreAcento else MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciales(nombre),
            style = when {
                tamano < 28.dp -> MaterialTheme.typography.labelSmall
                tamano < 40.dp -> MaterialTheme.typography.labelMedium
                else -> MaterialTheme.typography.titleMedium
            },
            color = texto,
            maxLines = 1
        )
    }
}

/** «Luis Ramírez» → «LR»; «Sin técnico» → «ST». Solo letras, como máximo dos. */
internal fun iniciales(nombre: String): String =
    nombre.split(' ')
        .mapNotNull { palabra -> palabra.firstOrNull { it.isLetter() } }
        .take(2)
        .joinToString("")
        .uppercase(Locale.forLanguageTag("es-PE"))
        .ifEmpty { "?" }

@Composable
private fun AvataresDePrueba() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.padding(Espaciado.m),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar("Luis Ramírez", esUsuarioActual = true)
            Avatar("Ana Torres")
            Avatar("Luis Ramírez", tamano = 48.dp, esUsuarioActual = true)
            Avatar("Ana Torres", tamano = 24.dp)
        }
    }
}

@Preview(name = "Avatares, claro", showBackground = true)
@Composable
private fun AvataresClaro() = SigtecTheme { AvataresDePrueba() }

@Preview(name = "Avatares, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvataresOscuro() = SigtecTheme(darkTheme = true) { AvataresDePrueba() }
