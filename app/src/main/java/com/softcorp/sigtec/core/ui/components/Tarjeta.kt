package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Tarjeta v2 (DESIGN.md, sección 3): blanca, con esquinas de 20 dp y sombra de 2 dp. En oscuro la sombra no se ve,
 * así que lleva el borde de `SigtecTheme.marca.bordeTarjeta` (plan del retema, sección 7). Con [onClick] toda la
 * tarjeta se puede tocar.
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    forma: Shape = MaterialTheme.shapes.medium,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val borde = BorderStroke(1.dp, SigtecTheme.marca.bordeTarjeta)
    val cuerpo: @Composable () -> Unit = { Column(Modifier.padding(Espaciado.m), content = contenido) }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = forma,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Elevacion.tarjeta,
            border = borde,
            modifier = modifier,
            content = cuerpo
        )
    } else {
        Surface(
            shape = forma,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Elevacion.tarjeta,
            border = borde,
            modifier = modifier,
            content = cuerpo
        )
    }
}

@Composable
private fun TarjetaDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Tarjeta(Modifier.padding(Espaciado.m), onClick = {}) {
            Text("Atendiendo ahora", style = MaterialTheme.typography.titleSmall)
            Text("No enciende después del corte de luz", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Tarjeta, claro", showBackground = true)
@Composable
private fun TarjetaClaro() = SigtecTheme { TarjetaDePrueba() }

@Preview(name = "Tarjeta, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TarjetaOscuro() = SigtecTheme(darkTheme = true) { TarjetaDePrueba() }
