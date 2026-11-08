package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/** DESIGN.md 4: neutra, acento (lo que se está atendiendo) o crítica (lo demorado). */
enum class VarianteMetrica { NEUTRA, ACENTO, CRITICA }

private const val OPACIDAD_EN_CABECERA = 0.12f
private val AltoMinimo = 88.dp

/**
 * Número grande con su etiqueta (DESIGN.md 4, prototipos 02 y 10). Con [enCabecera] la neutra es translúcida sobre
 * la cabecera oscura; fuera de ella es una tarjeta blanca. TalkBack la lee como una sola frase («3 Asignadas»).
 */
@Composable
fun TarjetaMetrica(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    variante: VarianteMetrica = VarianteMetrica.NEUTRA,
    enCabecera: Boolean = false
) {
    val marca = SigtecTheme.marca
    val c = MaterialTheme.colorScheme
    val (fondo, texto) = when (variante) {
        VarianteMetrica.ACENTO -> marca.acento to marca.sobreAcento
        VarianteMetrica.CRITICA -> c.errorContainer to c.onErrorContainer
        VarianteMetrica.NEUTRA ->
            if (enCabecera) marca.sobreHero.copy(alpha = OPACIDAD_EN_CABECERA) to marca.sobreHero
            else c.surface to c.onSurface
    }
    val tarjetaBlanca = variante == VarianteMetrica.NEUTRA && !enCabecera
    Surface(
        color = fondo,
        contentColor = texto,
        shape = MaterialTheme.shapes.large,
        shadowElevation = if (tarjetaBlanca) Elevacion.tarjeta else 0.dp,
        border = if (tarjetaBlanca) BorderStroke(1.dp, marca.bordeTarjeta) else null,
        modifier = modifier
            .heightIn(min = AltoMinimo)
            .semantics(mergeDescendants = true) { contentDescription = "$valor $etiqueta" }
    ) {
        Column(
            Modifier
                .padding(Espaciado.m)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
        ) {
            Text(valor, style = MaterialTheme.typography.headlineLarge, color = texto)
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = texto)
        }
    }
}

@Composable
private fun MetricasDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(Modifier.padding(Espaciado.m), horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            TarjetaMetrica("12", "Registradas", Modifier.weight(1f))
            TarjetaMetrica("4", "En atención", Modifier.weight(1f), VarianteMetrica.ACENTO)
            TarjetaMetrica("2", "Críticas", Modifier.weight(1f), VarianteMetrica.CRITICA)
        }
    }
}

@Preview(name = "Métricas, claro", showBackground = true)
@Composable
private fun MetricasClaro() = SigtecTheme { MetricasDePrueba() }

@Preview(name = "Métricas, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MetricasOscuro() = SigtecTheme(darkTheme = true) { MetricasDePrueba() }
