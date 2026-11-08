package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Título de sección («Detalles», «Actividad»): `titleMedium`, en negrita y sin mayúsculas, como «Atendiendo
 * ahora» del prototipo 02 (plan del retema, P-8).
 */
@Composable
fun EncabezadoSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() }
    )
}

/** Una sección con su encabezado y su contenido, separada de la siguiente por `Espaciado.l`. */
@Composable
fun Seccion(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        EncabezadoSeccion(titulo, Modifier.padding(bottom = Espaciado.xs))
        contenido()
    }
}

@Composable
private fun SeccionDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Seccion("Actividad", Modifier.padding(Espaciado.m)) {
            Text("Asignada a Luis Ramírez", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Sección, claro", showBackground = true)
@Composable
private fun SeccionClaro() = SigtecTheme { SeccionDePrueba() }

@Preview(name = "Sección, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeccionOscuro() = SigtecTheme(darkTheme = true) { SeccionDePrueba() }
