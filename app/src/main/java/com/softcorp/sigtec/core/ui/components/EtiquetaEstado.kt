package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.AltoEtiqueta
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

private val TamanoPunto = 8.dp
private val TamanoIcono = 16.dp

/**
 * Etiqueta de estado v2 (DESIGN.md, sección 4): píldora de 28 dp con un punto de color y el texto en
 * `labelMedium`, sin mayúsculas. Con [icono] (por ejemplo, la alerta de «Crítico») el ícono reemplaza al punto.
 * Siempre lleva texto: el estado nunca depende solo del color.
 */
@Composable
fun EtiquetaEstado(
    texto: String,
    color: ColorEstado,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    Surface(
        color = color.contenedor,
        shape = FormaCompleta,
        modifier = modifier.heightIn(min = AltoEtiqueta)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.xs)
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = color.punto, modifier = Modifier.size(TamanoIcono))
            } else {
                Box(
                    Modifier
                        .size(TamanoPunto)
                        .clip(CircleShape)
                        .background(color.punto)
                )
            }
            Text(
                text = texto,
                color = color.contenido,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EtiquetasDePrueba() {
    val e = SigtecTheme.estados
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            EtiquetaEstado("Sin asignar", e.sinAsignar)
            EtiquetaEstado("Asignada", e.asignada)
            EtiquetaEstado("En atención", e.enAtencion)
            EtiquetaEstado("Esperando repuesto, 4 días", e.esperandoRepuesto)
            EtiquetaEstado("Solucionado", e.solucionado)
            EtiquetaEstado("Crítico, 6 días", e.critico, icono = Icons.Outlined.ErrorOutline)
        }
    }
}

@Preview(name = "Etiquetas, claro", showBackground = true)
@Composable
private fun EtiquetasClaro() = SigtecTheme { EtiquetasDePrueba() }

@Preview(name = "Etiquetas, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EtiquetasOscuro() = SigtecTheme(darkTheme = true) { EtiquetasDePrueba() }
