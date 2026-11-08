package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaHoja
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Hoja blanca pegada abajo con la acción principal (DESIGN.md 4, prototipo 05): esquinas superiores de 32 dp y
 * sombra marcada. Se pasa como `bottomBar` del `Scaffold`. [aviso] va encima de los botones, por ejemplo un error.
 */
@Composable
fun BarraAccionInferior(
    modifier: Modifier = Modifier,
    aviso: (@Composable () -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = FormaHoja,
        shadowElevation = Elevacion.flotante,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            aviso?.invoke()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
                verticalAlignment = Alignment.CenterVertically,
                content = acciones
            )
        }
    }
}

@Composable
private fun BarraDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.BottomCenter) {
            BarraAccionInferior {
                BotonSecundario(onClick = {}, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null)
                    Text("Repuesto", modifier = Modifier.padding(start = Espaciado.s))
                }
                BotonPrincipal(onClick = {}, modifier = Modifier.weight(1.5f)) { Text("Cerrar con informe") }
            }
        }
    }
}

@Preview(name = "Barra de acción, claro", showBackground = true, heightDp = 240)
@Composable
private fun BarraAccionClaro() = SigtecTheme { BarraDePrueba() }

@Preview(name = "Barra de acción, oscuro", showBackground = true, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarraAccionOscuro() = SigtecTheme(darkTheme = true) { BarraDePrueba() }
