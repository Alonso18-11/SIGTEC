package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

private val TamanoNumero = 32.dp

/**
 * Sugerencia generada por IA (DESIGN.md principio 5 y sección 4): contenedor `tertiaryContainer`, ícono de destello y
 * etiqueta «IA», para que nunca se confunda con un dato. La IA solo sugiere: con [alUsar] y [alDescartar] la persona
 * acepta o descarta (la decisión se guarda en `SugerenciaIA.aceptada`). [textoDescartar] puede ser «Pedir cambios».
 * Con [numero] se muestra como una opción numerada, como en «Qué probar primero».
 */
@Composable
fun TarjetaSugerenciaIA(
    texto: String,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    numero: Int? = null,
    alUsar: (() -> Unit)? = null,
    alDescartar: (() -> Unit)? = null,
    textoDescartar: String = "Descartar"
) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.tertiaryContainer, contentColor = c.onTertiaryContainer, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.sm), verticalAlignment = Alignment.Top) {
                if (numero != null) {
                    Box(
                        Modifier.size(TamanoNumero).clip(CircleShape).background(c.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$numero", style = MaterialTheme.typography.labelLarge, color = c.onTertiary)
                    }
                } else {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.tertiary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                    titulo?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                    Text(texto, style = MaterialTheme.typography.bodyMedium)
                }
                if (numero == null) EtiquetaIA()
            }
            if (alUsar != null || alDescartar != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    alDescartar?.let { BotonTexto(onClick = it, color = c.onTertiaryContainer) { Text(textoDescartar) } }
                    alUsar?.let { BotonTexto(onClick = it, color = c.tertiary) { Text("Usar") } }
                }
            }
        }
    }
}

/** La etiqueta «IA» que acompaña a todo lo generado (DESIGN.md, principio 5). */
@Composable
fun EtiquetaIA(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.tertiary, shape = FormaCompleta, modifier = modifier) {
        Text("IA", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = Espaciado.s, vertical = 2.dp))
    }
}

@Composable
private fun SugerenciasDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            TarjetaSugerenciaIA(
                titulo = "Hardware, prioridad alta",
                texto = "El jefe del área confirma o corrige la clasificación al asignar.",
                alUsar = {}, alDescartar = {}
            )
            TarjetaSugerenciaIA(
                numero = 1,
                titulo = "Fuente de poder dañada por voltaje",
                texto = "Probar con una fuente de reemplazo y revisar el estabilizador."
            )
            TarjetaSugerenciaIA(
                texto = "Se reemplazó la fuente de poder y el equipo enciende con normalidad.",
                alUsar = {}, alDescartar = {}, textoDescartar = "Pedir cambios"
            )
        }
    }
}

@Preview(name = "Sugerencia IA, claro", showBackground = true)
@Composable
private fun SugerenciaClaro() = SigtecTheme { SugerenciasDePrueba() }

@Preview(name = "Sugerencia IA, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SugerenciaOscuro() = SigtecTheme(darkTheme = true) { SugerenciasDePrueba() }
