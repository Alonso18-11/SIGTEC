package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.ColorEstado
import java.util.Locale

private val Espanol = Locale.forLanguageTag("es-PE")

/**
 * «Lozenge» de estado al estilo Jira: texto corto en mayúsculas sobre un fondo suave del color del estado.
 * Siempre lleva texto: el estado nunca depende solo del color (sección 12.2).
 */
@Composable
fun EtiquetaEstado(
    texto: String,
    color: ColorEstado,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.contenedor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = texto.uppercase(Espanol),
            color = color.contenido,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
