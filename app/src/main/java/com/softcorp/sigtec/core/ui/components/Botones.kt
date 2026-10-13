package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado

// Botones al estilo Jira. Material 3 no toma la forma de los botones del tema (siempre son
// «píldoras»), así que se fija aquí una sola vez: esquinas de 4 dp y sin sombra.

private val RellenoBoton = PaddingValues(horizontal = Espaciado.m, vertical = Espaciado.s)

/** La acción principal de la pantalla: azul sólido. Debería haber solo uno visible a la vez. */
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
        shape = MaterialTheme.shapes.small,
        contentPadding = RellenoBoton,
        modifier = modifier.defaultMinSize(minHeight = AreaTactilMinima),
        content = content
    )
}

/** Acción secundaria: gris neutro, como el botón por defecto de Jira (no un botón con contorno). */
@Composable
fun BotonSecundario(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        contentPadding = RellenoBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.defaultMinSize(minHeight = AreaTactilMinima),
        content = content
    )
}

/** Acción de bajo peso (diálogos, «Limpiar filtros»): solo texto en azul. */
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
        shape = MaterialTheme.shapes.small,
        contentPadding = relleno,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        modifier = modifier,
        content = content
    )
}

/** Acción destructiva (cerrar sesión): rojo suave, como el botón «danger» atenuado de Jira. */
@Composable
fun BotonPeligro(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        contentPadding = RellenoBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = modifier.defaultMinSize(minHeight = AreaTactilMinima),
        content = content
    )
}
