package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCabecera
import com.softcorp.sigtec.core.theme.SigtecTheme

private const val OPACIDAD_CIRCULO = 0.10f
private const val OPACIDAD_ANILLO = 0.08f

/**
 * Cabecera oscura v2 (DESIGN.md, sección 4): fondo `hero`, esquinas inferiores de 32 dp y círculos decorativos.
 * Los círculos son solo decoración y no llegan a TalkBack. El contenido toma `sobreHero` como color de texto.
 * La usan Inicio, Detalle, Indicadores, Historial, Login y Dictado.
 */
@Composable
fun CabeceraHero(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val marca = SigtecTheme.marca
    Box(
        modifier
            .fillMaxWidth()
            .clip(FormaCabecera)
            .background(marca.hero)
    ) {
        Canvas(
            Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
        ) {
            val radio = size.minDimension * 0.42f
            val centro = Offset(size.width - radio * 0.35f, radio * 0.35f)
            drawCircle(marca.sobreHero.copy(alpha = OPACIDAD_CIRCULO), radius = radio * 0.75f, center = centro)
            drawCircle(
                marca.sobreHero.copy(alpha = OPACIDAD_ANILLO),
                radius = radio * 1.25f,
                center = centro,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        CompositionLocalProvider(LocalContentColor provides marca.sobreHero) {
            Column(
                Modifier.padding(start = Espaciado.m, end = Espaciado.m, top = Espaciado.m, bottom = Espaciado.l),
                verticalArrangement = Arrangement.spacedBy(Espaciado.m),
                content = contenido
            )
        }
    }
}

@Composable
private fun CabeceraDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        CabeceraHero {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
                Avatar("Luis Ramírez", tamano = 48.dp, esUsuarioActual = true)
                Column {
                    Text("Técnico de soporte", style = MaterialTheme.typography.bodyMedium, color = SigtecTheme.marca.sobreHeroTenue)
                    Text("Buenos días, Luis", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                TarjetaMetrica("3", "Asignadas", Modifier.weight(1f), enCabecera = true)
                TarjetaMetrica("1", "En atención", Modifier.weight(1f), VarianteMetrica.ACENTO, enCabecera = true)
                TarjetaMetrica("1", "Esperando repuesto", Modifier.weight(1f), enCabecera = true)
            }
        }
    }
}

@Preview(name = "Cabecera, claro", showBackground = true)
@Composable
private fun CabeceraClaro() = SigtecTheme { CabeceraDePrueba() }

@Preview(name = "Cabecera, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CabeceraOscuro() = SigtecTheme(darkTheme = true) { CabeceraDePrueba() }
