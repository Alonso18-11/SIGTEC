package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Pestañas segmentadas (DESIGN.md 4, prototipo 06): pista `surfaceVariant` y la opción activa blanca con sombra.
 * Cada opción mide 48 dp de alto o más.
 */
@Composable
fun <T> PestanasSegmentadas(
    opciones: List<T>,
    seleccionada: T,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceVariant, shape = FormaCompleta, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(Espaciado.xs)) {
            opciones.forEach { opcion ->
                val activa = opcion == seleccionada
                Surface(
                    selected = activa,
                    onClick = { alElegir(opcion) },
                    shape = FormaCompleta,
                    color = if (activa) c.surface else Color.Transparent,
                    contentColor = if (activa) c.primary else c.onSurfaceVariant,
                    shadowElevation = if (activa) Elevacion.tarjeta else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AreaTactilMinima)
                        .semantics { role = Role.Tab }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(texto(opcion), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PestanasDePrueba() {
    var elegida by remember { mutableStateOf("Pendientes") }
    Surface(color = MaterialTheme.colorScheme.background) {
        PestanasSegmentadas(
            opciones = listOf("Pendientes", "Repuestos (2)"),
            seleccionada = elegida,
            texto = { it },
            alElegir = { elegida = it },
            modifier = Modifier.padding(Espaciado.m)
        )
    }
}

@Preview(name = "Pestañas, claro", showBackground = true)
@Composable
private fun PestanasClaro() = SigtecTheme { PestanasDePrueba() }

@Preview(name = "Pestañas, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PestanasOscuro() = SigtecTheme(darkTheme = true) { PestanasDePrueba() }
