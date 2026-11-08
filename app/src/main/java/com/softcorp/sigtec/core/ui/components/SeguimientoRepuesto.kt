package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/** Los tres pasos de una solicitud a Logística (DESIGN.md 4, prototipos 06 y 13). */
val PasosRepuesto = listOf("Solicitado", "Despachado", "Entregado")

private val TamanoPunto = 14.dp
private val GrosorLinea = 4.dp
private const val OPACIDAD_PENDIENTE = 0.35f

/**
 * Seguimiento de un repuesto (DESIGN.md 4): tres puntos unidos por una línea, en violeta mientras llega y en verde al
 * entregarse. [hechos] es cuántos pasos ya ocurrieron (1 = solo solicitado, 3 = entregado). [titulo] y [detalle]
 * van arriba, por ejemplo «Esperando repuesto, 4 días» y «Llega mañana».
 */
@Composable
fun SeguimientoRepuesto(
    hechos: Int,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    detalle: String? = null,
    pasos: List<String> = PasosRepuesto
) {
    val entregado = hechos >= pasos.size
    val estado = if (entregado) SigtecTheme.estados.solucionado else SigtecTheme.estados.esperandoRepuesto
    val pendiente = estado.punto.copy(alpha = OPACIDAD_PENDIENTE)
    val descripcion = listOfNotNull(titulo, detalle, pasos.getOrNull(hechos - 1)).joinToString(". ")
    Surface(
        color = estado.contenedor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
    ) {
        Column(
            Modifier
                .padding(Espaciado.m)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            if (titulo != null || detalle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titulo?.let {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(estado.punto))
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = estado.contenido,
                            modifier = Modifier.padding(start = Espaciado.s).weight(1f)
                        )
                    }
                    detalle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = estado.contenido) }
                }
            }
            // Puntos y líneas
            Row(verticalAlignment = Alignment.CenterVertically) {
                pasos.indices.forEach { i ->
                    val hecho = i < hechos
                    Box(
                        Modifier
                            .size(TamanoPunto)
                            .clip(CircleShape)
                            .background(if (hecho) estado.punto else estado.contenedor)
                            .border(2.dp, estado.punto, CircleShape)
                    )
                    if (i < pasos.lastIndex) {
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = Espaciado.xs)
                                .height(GrosorLinea)
                                .clip(CircleShape)
                                .background(if (i + 1 < hechos) estado.punto else pendiente)
                        )
                    }
                }
            }
            Row {
                pasos.forEachIndexed { i, paso ->
                    Text(
                        paso,
                        style = MaterialTheme.typography.bodySmall,
                        color = estado.contenido,
                        textAlign = when (i) {
                            0 -> TextAlign.Start
                            pasos.lastIndex -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeguimientoDePrueba() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            SeguimientoRepuesto(hechos = 2, titulo = "Esperando repuesto, 4 días", detalle = "Llega mañana")
            SeguimientoRepuesto(hechos = 3, titulo = "Entregado", detalle = "09/11")
            SeguimientoRepuesto(hechos = 1)
        }
    }
}

@Preview(name = "Seguimiento, claro", showBackground = true)
@Composable
private fun SeguimientoClaro() = SigtecTheme { SeguimientoDePrueba() }

@Preview(name = "Seguimiento, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeguimientoOscuro() = SigtecTheme(darkTheme = true) { SeguimientoDePrueba() }
