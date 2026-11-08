package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Botón flotante con texto (DESIGN.md 4): fondo `acento` con `sobreAcento` encima, como «Nueva» o «Registrar falla».
 * Es uno de los pocos usos permitidos del ámbar.
 */
@Composable
fun FabExtendido(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Outlined.Add
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icono, contentDescription = null) },
        text = { Text(texto, style = MaterialTheme.typography.labelLarge) },
        shape = FormaCompleta,
        containerColor = SigtecTheme.marca.acento,
        contentColor = SigtecTheme.marca.sobreAcento,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Elevacion.flotante),
        modifier = modifier
    )
}

@Composable
private fun FabDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.padding(Espaciado.m)) { FabExtendido("Nueva", onClick = {}) }
    }
}

@Preview(name = "FAB, claro", showBackground = true)
@Composable
private fun FabClaro() = SigtecTheme { FabDePrueba() }

@Preview(name = "FAB, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FabOscuro() = SigtecTheme(darkTheme = true) { FabDePrueba() }
