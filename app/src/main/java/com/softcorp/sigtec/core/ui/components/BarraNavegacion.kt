package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.SigtecTheme

/** Una pestaña del menú inferior. Quién ve cada una lo decide `DestinoPrincipal` por permisos, no este componente. */
data class PestanaNavegacion(
    val etiqueta: String,
    val icono: ImageVector,
    val seleccionada: Boolean,
    val alPulsar: () -> Unit
)

/**
 * Menú inferior v2 (DESIGN.md 4): `NavigationBar` de Material 3 sobre blanco, con la pastilla `primaryContainer`
 * detrás del ícono de la pestaña activa.
 */
@Composable
fun BarraNavegacion(pestanas: List<PestanaNavegacion>, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier) {
        HorizontalDivider(color = c.outlineVariant)
        NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
            pestanas.forEach { pestana ->
                NavigationBarItem(
                    selected = pestana.seleccionada,
                    onClick = pestana.alPulsar,
                    icon = { Icon(pestana.icono, contentDescription = null) },
                    label = { Text(pestana.etiqueta, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = c.primaryContainer,
                        selectedIconColor = c.onPrimaryContainer,
                        selectedTextColor = c.onSurface,
                        unselectedIconColor = c.onSurfaceVariant,
                        unselectedTextColor = c.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun BarraNavegacionDePrueba() {
    BarraNavegacion(
        listOf(
            PestanaNavegacion("Inicio", Icons.Outlined.Home, seleccionada = true) {},
            PestanaNavegacion("Incidencias", Icons.AutoMirrored.Outlined.List, seleccionada = false) {}
        )
    )
}

@Preview(name = "Menú inferior, claro", showBackground = true)
@Composable
private fun BarraNavegacionClaro() = SigtecTheme { BarraNavegacionDePrueba() }

@Preview(name = "Menú inferior, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarraNavegacionOscuro() = SigtecTheme(darkTheme = true) { BarraNavegacionDePrueba() }
