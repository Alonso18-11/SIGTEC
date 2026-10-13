package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

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
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        EtiquetaEstado(texto = "Pendiente · $historia", color = SigtecTheme.estados.asignada)
        if (acciones.isNotEmpty()) {
            Spacer(Modifier.height(Espaciado.m))
            acciones.forEach { (texto, alPulsar) ->
                BotonSecundario(onClick = alPulsar, modifier = Modifier.fillMaxWidth()) { Text(texto) }
            }
        }
    }
}

@Preview(name = "En construcción · claro", showBackground = true)
@Composable
private fun EnConstruccionClaro() = SigtecTheme {
    Surface { PantallaEnConstruccion("Hola, Luis · Técnico", "panel del perfil", acciones = listOf("Registrar incidencia" to {})) }
}

@Preview(name = "En construcción · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EnConstruccionOscuro() = SigtecTheme(darkTheme = true) {
    Surface { PantallaEnConstruccion("Hola, Luis · Técnico", "panel del perfil", acciones = listOf("Registrar incidencia" to {})) }
}
