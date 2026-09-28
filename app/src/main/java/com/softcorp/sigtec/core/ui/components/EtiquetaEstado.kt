package com.softcorp.sigtec.core.ui.componentes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.Espaciado

// Siempre lleva texto: el estado nunca depende solo del color (sección 12.2).
@Composable
fun EtiquetaEstado(
    texto: String,
    color: ColorEstado,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.contenedor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = texto,
            color = color.contenido,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Espaciado.s, vertical = Espaciado.xs)
        )
    }
}