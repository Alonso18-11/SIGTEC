package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/** DESIGN.md 4: verde = solucionado; ámbar con halo = en curso; el resto, en `primary`. */
enum class PuntoLinea { NORMAL, SOLUCIONADO, EN_CURSO }

/** Un punto de la línea de tiempo: título, un detalle opcional («Ana Torres, 19/10») y su punto. */
data class EventoLinea(val titulo: String, val detalle: String? = null, val punto: PuntoLinea = PuntoLinea.NORMAL)

private val TamanoPunto = 12.dp
private val AnchoColumna = 24.dp
private const val OPACIDAD_HALO = 0.35f

/**
 * Línea de tiempo vertical (DESIGN.md 4, prototipo 11): un punto por evento unido al siguiente por una línea.
 * Cada evento es un solo elemento para TalkBack.
 */
@Composable
fun LineaTiempo(eventos: List<EventoLinea>, modifier: Modifier = Modifier) {
    Column(modifier) {
        eventos.forEachIndexed { i, evento ->
            Row(Modifier.height(IntrinsicSize.Min).semantics(mergeDescendants = true) {}) {
                Column(
                    Modifier.width(AnchoColumna).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Punto(evento.punto, Modifier.padding(top = Espaciado.xs))
                    if (i < eventos.lastIndex) {
                        Box(
                            Modifier
                                .padding(vertical = Espaciado.xs)
                                .width(2.dp)
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(
                    Modifier.padding(start = Espaciado.sm, bottom = if (i < eventos.lastIndex) Espaciado.m else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(evento.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    evento.detalle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Punto(tipo: PuntoLinea, modifier: Modifier = Modifier) {
    val color = when (tipo) {
        PuntoLinea.SOLUCIONADO -> SigtecTheme.estados.solucionado.punto
        PuntoLinea.EN_CURSO -> SigtecTheme.marca.acento
        PuntoLinea.NORMAL -> MaterialTheme.colorScheme.primary
    }
    if (tipo == PuntoLinea.EN_CURSO) {
        Box(
            modifier
                .size(TamanoPunto + 8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = OPACIDAD_HALO)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(TamanoPunto).clip(CircleShape).background(color))
        }
    } else {
        Box(modifier.size(TamanoPunto).clip(CircleShape).background(color))
    }
}

@Composable
private fun LineaDePrueba() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        LineaTiempo(
            listOf(
                EventoLinea("En atención", "Luis Ramírez, hoy 09:32", PuntoLinea.EN_CURSO),
                EventoLinea("Fuente de poder cambiada", "Ana Torres, 02/10", PuntoLinea.SOLUCIONADO),
                EventoLinea("Incidencia registrada", "Carlos Mendoza, 28/09")
            ),
            Modifier.padding(Espaciado.m)
        )
    }
}

@Preview(name = "Línea de tiempo, claro", showBackground = true)
@Composable
private fun LineaClaro() = SigtecTheme { LineaDePrueba() }

@Preview(name = "Línea de tiempo, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LineaOscuro() = SigtecTheme(darkTheme = true) { LineaDePrueba() }
