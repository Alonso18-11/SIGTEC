package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.softcorp.sigtec.core.theme.Espaciado

// TEMPORAL: cada pantalla la reemplaza cuando se implementa su HU.
@Composable
fun PantallaEnConstruccion(
    titulo: String,
    historia: String,
    modifier: Modifier = Modifier,
    acciones: List<Pair<String, () -> Unit>> = emptyList()
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Espaciado.m),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        Text(
            "Pendiente · $historia",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Espaciado.m))
        acciones.forEach { (texto, alPulsar) ->
            OutlinedButton(onClick = alPulsar, modifier = Modifier.fillMaxWidth()) { Text(texto) }
        }
    }
}