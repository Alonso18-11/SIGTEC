package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.statusBars
import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Barra superior de formularios y listas secundarias (DESIGN.md, sección 4). En la v2 va sobre el color de
 * fondo de la pantalla y sin línea inferior (plan del retema, P-6), así el título se ve como en los prototipos.
 * Aplica el margen de la barra de estado (la raíz ya no lo hace, para que las cabeceras oscuras lleguen arriba); la
 * pantalla solo debe usar `Scaffold(contentWindowInsets = WindowInsets(0))`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        windowInsets = WindowInsets.statusBars
    )
}

@Composable
private fun BarrasDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        BarraSuperior("Nueva incidencia", alVolver = {}) {
            IconButton(onClick = {}) { Icon(Icons.Outlined.FlashlightOn, contentDescription = "Encender linterna") }
        }
    }
}

@Preview(name = "Barra superior, claro", showBackground = true)
@Composable
private fun BarraClaro() = SigtecTheme { BarrasDePrueba() }

@Preview(name = "Barra superior, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarraOscuro() = SigtecTheme(darkTheme = true) { BarrasDePrueba() }
