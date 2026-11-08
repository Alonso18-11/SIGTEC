package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.AltoBoton
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme

// Botones v2 (DESIGN.md, sección 4). Material 3 no toma la forma de los botones del tema, así que se fija
// aquí una sola vez: forma completa, 56 dp de alto y sin sombra. El de texto mide 48 dp porque va en línea,
// dentro de avisos y filas (plan del retema, P-7).

// Mismo relleno lateral que antes: con más, «Cerrar con informe» deja de caber junto a «Repuesto» en el detalle
private val RellenoBoton = PaddingValues(horizontal = Espaciado.m, vertical = Espaciado.s)
private val BordeBoton = 1.5.dp
private const val OPACIDAD_DESHABILITADO = 0.12f

/** La acción principal de la pantalla, en `primary`. Solo uno visible a la vez (DESIGN.md, principio 2). */
@Composable
fun BotonPrincipal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = FormaCompleta,
        contentPadding = RellenoBoton,
        modifier = modifier.defaultMinSize(minHeight = AltoBoton),
        content = content
    )
}

/** Acción secundaria: borde y texto en `primary`, sin relleno. */
@Composable
fun BotonSecundario(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    BotonConBorde(onClick, MaterialTheme.colorScheme.primary, modifier, enabled, content)
}

/** Acción de bajo peso («Usar», «Reintentar», «Limpiar filtros»): solo texto. */
@Composable
fun BotonTexto(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    relleno: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = FormaCompleta,
        contentPadding = relleno,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        modifier = modifier.defaultMinSize(minHeight = AreaTactilMinima),
        content = content
    )
}

/** Acción destructiva (cerrar sesión, descartar): borde y texto en `error`. */
@Composable
fun BotonPeligro(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    BotonConBorde(onClick, MaterialTheme.colorScheme.error, modifier, enabled, content)
}

@Composable
private fun BotonConBorde(
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = FormaCompleta,
        contentPadding = RellenoBoton,
        border = BorderStroke(
            BordeBoton,
            if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = OPACIDAD_DESHABILITADO)
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier.defaultMinSize(minHeight = AltoBoton),
        content = content
    )
}

// ---------- Vistas previas ----------

@Composable
private fun Botones() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            BotonPrincipal(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Cerrar con informe") }
            BotonSecundario(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Repuesto") }
            BotonPeligro(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
            BotonTexto(onClick = {}) { Text("Usar") }
            BotonPrincipal(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Guardar incidencia") }
            BotonSecundario(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Ingresar con huella") }
        }
    }
}

@Preview(name = "Botones, claro", showBackground = true)
@Composable
private fun BotonesClaro() = SigtecTheme { Botones() }

@Preview(name = "Botones, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BotonesOscuro() = SigtecTheme(darkTheme = true) { Botones() }
