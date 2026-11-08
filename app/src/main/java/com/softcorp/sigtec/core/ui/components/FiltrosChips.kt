package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.AltoChip
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Fila de chips de filtro (DESIGN.md 4, prototipo 03): una sola opción activa, rellena en `primary`. Cada chip se
 * dibuja de 40 dp dentro de un área tocable de 48 dp. La fila se desplaza de lado si no cabe; [relleno] es el margen
 * de los extremos, para que el primer chip quede alineado con el resto de la pantalla.
 */
@Composable
fun <T> FiltrosChips(
    opciones: List<T>,
    seleccionada: T,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(horizontal = Espaciado.m)
) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(relleno),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        opciones.forEach { opcion ->
            Chip(texto(opcion), activa = opcion == seleccionada, alPulsar = { alElegir(opcion) })
        }
    }
}

@Composable
private fun Chip(texto: String, activa: Boolean, alPulsar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Surface(
            selected = activa,
            onClick = alPulsar,
            shape = FormaCompleta,
            color = if (activa) c.primary else c.surface,
            contentColor = if (activa) c.onPrimary else c.onSurface,
            border = if (activa) null else BorderStroke(1.dp, c.outlineVariant),
            modifier = Modifier
                .height(AltoChip)
                .semantics { role = Role.RadioButton }
        ) {
            Box(Modifier.padding(horizontal = Espaciado.m), contentAlignment = Alignment.Center) {
                Text(texto, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FiltrosDePrueba() {
    var elegida by remember { mutableStateOf("Todas (5)") }
    Surface(color = MaterialTheme.colorScheme.background) {
        FiltrosChips(
            opciones = listOf("Todas (5)", "Sin asignar (1)", "En atención (1)", "Críticas (1)"),
            seleccionada = elegida,
            texto = { it },
            alElegir = { elegida = it },
            modifier = Modifier.padding(vertical = Espaciado.s)
        )
    }
}

@Preview(name = "Filtros, claro", showBackground = true)
@Composable
private fun FiltrosClaro() = SigtecTheme { FiltrosDePrueba() }

@Preview(name = "Filtros, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FiltrosOscuro() = SigtecTheme(darkTheme = true) { FiltrosDePrueba() }
