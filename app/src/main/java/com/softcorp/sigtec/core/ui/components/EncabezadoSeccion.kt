package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.softcorp.sigtec.core.theme.Espaciado
import java.util.Locale

private val Espanol = Locale.forLanguageTag("es-PE")

/** Título de sección pequeño, gris y en mayúsculas (DETALLES, ACTIVIDAD…), como en un issue de Jira. */
@Composable
fun EncabezadoSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(Espanol),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
