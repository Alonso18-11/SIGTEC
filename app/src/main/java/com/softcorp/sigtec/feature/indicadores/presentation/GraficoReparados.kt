package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.feature.indicadores.domain.ReparadosDia

private val AltoGrafico = 180.dp
private val AnchoColumna = 28.dp
private const val RADIO_COLUMNA = 8f

/**
 * RF-1509, RF-1510, RF-1513: columnas de Vico de los últimos 7 días, con el número encima y el día debajo. Hoy va en
 * acento y los demás días en el color principal (prototipo 10); el día también está en texto. TalkBack lee los 7
 * valores en una sola descripción.
 */
@Composable
fun GraficoReparados(dias: List<ReparadosDia>, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val forma = remember { CorneredShape.rounded(RADIO_COLUMNA, RADIO_COLUMNA, 0f, 0f) }
    val normal = rememberLineComponent(fill(c.primary), AnchoColumna, forma)
    val hoy = rememberLineComponent(fill(SigtecTheme.marca.acento), AnchoColumna, forma)
    val indiceHoy = dias.indexOfFirst { it.esHoy }
    val columnas = remember(normal, hoy, indiceHoy) { ColumnaDeHoy(normal, hoy, indiceHoy) }
    val etiquetaColor = c.onSurface
    val ejeColor = c.onSurfaceVariant

    val etiquetas = remember(dias) { dias.map { diaCorto(it.dia) } }
    val modelo = remember(dias) {
        CartesianChartModel(ColumnCartesianLayerModel.build { series(dias.map { it.cantidad }) })
    }
    // Sin cierres, el eje vertical llega a 1 para que las columnas en 0 se dibujen igual (RF-1513)
    val maximo = (dias.maxOfOrNull { it.cantidad } ?: 0).coerceAtLeast(1).toDouble()
    val descripcion = dias.joinToString(", ", prefix = "Equipos reparados por día: ") {
        "${diaLargo(it.dia)} ${it.cantidad}"
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = columnas,
                dataLabel = rememberTextComponent(color = etiquetaColor),
                rangeProvider = remember(maximo) { CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = maximo) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = rememberTextComponent(color = ejeColor),
                line = rememberLineComponent(fill(c.outlineVariant)),
                tick = null,
                guideline = null,
                valueFormatter = remember(etiquetas) {
                    CartesianValueFormatter { _, x, _ -> etiquetas.getOrElse(x.toInt()) { "" } }
                }
            )
        ),
        model = modelo,
        modifier = modifier
            .fillMaxWidth()
            .height(AltoGrafico)
            .semantics { contentDescription = descripcion }
    )
}

/** Una columna normal por día y la de hoy en acento (la x de cada columna es su posición, de 0 a 6). */
private class ColumnaDeHoy(
    private val normal: LineComponent,
    private val hoy: LineComponent,
    private val indiceHoy: Int
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(entry: ColumnCartesianLayerModel.Entry, seriesIndex: Int, extraStore: ExtraStore) =
        if (entry.x.toInt() == indiceHoy) hoy else normal

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore) = normal
}
