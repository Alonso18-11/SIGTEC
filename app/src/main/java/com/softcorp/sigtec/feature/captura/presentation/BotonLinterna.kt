package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * HU-10 (RF-1025, RF-1030): el botón de la linterna para la barra superior. El estado se ve en el ícono, no solo
 * en el color, y el lector de pantalla dice qué hará al tocarlo.
 */
@Composable
fun BotonLinterna(encendida: Boolean, alAlternar: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = alAlternar, modifier = modifier) {
        if (encendida) {
            Icon(
                Icons.Filled.FlashlightOn,
                contentDescription = "Apagar linterna",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Outlined.FlashlightOff,
                contentDescription = "Encender linterna",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Previa() = Surface {
    Row {
        BotonLinterna(encendida = false, alAlternar = {})
        BotonLinterna(encendida = true, alAlternar = {})
    }
}

@Preview(name = "Linterna, claro", showBackground = true)
@Composable
private fun LinternaClaro() = SigtecTheme { Previa() }

@Preview(name = "Linterna, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LinternaOscuro() = SigtecTheme(darkTheme = true) { Previa() }
