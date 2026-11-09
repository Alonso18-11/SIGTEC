package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/** Los cuatro pasos de la atención (DESIGN.md 4, prototipo 05). */
val PasosIncidencia = listOf("Asignada", "En atención", "Repuesto", "Cierre")

private val TamanoPaso = 32.dp
private val Halo = 6.dp
private const val OPACIDAD_HALO = 0.35f

/**
 * Avance de una incidencia (DESIGN.md 4): los pasos anteriores a [actual] van en `primary` con un check, el actual en
 * `acento` con un halo y los siguientes punteados. [actual] empieza en 0. TalkBack lo lee como una sola frase.
 */
@Composable
fun AvanceIncidencia(
    actual: Int,
    modifier: Modifier = Modifier,
    pasos: List<String> = PasosIncidencia
) {
    val descripcion = if (actual >= pasos.size) "Atención completada" else "Paso ${actual + 1} de ${pasos.size}: ${pasos[actual]}"
    Tarjeta(modifier.semantics(mergeDescendants = true) { contentDescription = descripcion }) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.SpaceBetween) {
            pasos.forEachIndexed { i, paso ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Espaciado.s)
                ) {
                    when {
                        i < actual -> PasoHecho()
                        i == actual -> PasoActual()
                        else -> PasoPendiente()
                    }
                    Text(
                        paso,
                        style = if (i <= actual) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                        color = when {
                            i < actual -> MaterialTheme.colorScheme.onSurface
                            i == actual -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PasoHecho() {
    Box(
        Modifier
            .size(TamanoPaso)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PasoActual() {
    val acento = SigtecTheme.marca.acento
    Box(
        Modifier
            .size(TamanoPaso)
            .clip(CircleShape)
            .background(acento.copy(alpha = OPACIDAD_HALO))
            .padding(Halo / 2)
            .clip(CircleShape)
            .background(acento)
    )
}

@Composable
private fun PasoPendiente() {
    val borde = MaterialTheme.colorScheme.outline
    val relleno = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(TamanoPaso)) {
        val grosor = 2.dp.toPx()
        drawCircle(relleno, radius = size.minDimension / 2 - grosor)
        drawCircle(
            borde,
            radius = size.minDimension / 2 - grosor / 2,
            style = Stroke(width = grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
        )
    }
}

@Composable
private fun AvanceDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            AvanceIncidencia(actual = 1)
            AvanceIncidencia(actual = 3)
        }
    }
}

@Preview(name = "Avance, claro", showBackground = true)
@Composable
private fun AvanceClaro() = SigtecTheme { AvanceDePrueba() }

@Preview(name = "Avance, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvanceOscuro() = SigtecTheme(darkTheme = true) { AvanceDePrueba() }
