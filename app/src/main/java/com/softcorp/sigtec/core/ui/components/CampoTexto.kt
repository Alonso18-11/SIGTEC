package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

/**
 * Campo de texto v2 (plan del retema, P-11 y P-12): fondo `surface` sobre el teal claro, esquinas de 16 dp y borde
 * `outline`, que llega a 3:1 (con `outlineVariant` no llegaba). La etiqueta siempre es visible (DESIGN.md 7).
 * [ayuda] va debajo; con [error] el borde y la ayuda pasan a `error`. [final] es, por ejemplo, el botón de cámara o
 * de micrófono. [acciones] responde a la tecla del teclado, por ejemplo «Listo» para enviar el formulario.
 */
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    final: (@Composable () -> Unit)? = null,
    ayuda: String? = null,
    error: Boolean = false,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None
) {
    val c = MaterialTheme.colorScheme
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        leadingIcon = icono?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = final,
        supportingText = ayuda?.let { { Text(it) } },
        isError = error,
        enabled = habilitado,
        singleLine = unaLinea,
        minLines = lineasMinimas,
        keyboardOptions = teclado,
        keyboardActions = acciones,
        visualTransformation = transformacion,
        shape = MaterialTheme.shapes.extraSmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
            disabledContainerColor = c.surface,
            errorContainerColor = c.surface,
            unfocusedBorderColor = c.outline,
            focusedBorderColor = c.primary,
            unfocusedLeadingIconColor = c.primary,
            focusedLeadingIconColor = c.primary,
            unfocusedTrailingIconColor = c.primary,
            focusedTrailingIconColor = c.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CamposDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            CampoTexto("", {}, "Correo institucional", icono = Icons.Outlined.Mail)
            CampoTexto(
                "No enciende después del corte de luz", {}, "Descripción del problema",
                unaLinea = false, lineasMinimas = 3,
                final = { IconButton(onClick = {}) { Icon(Icons.Outlined.Mic, contentDescription = "Dictar descripción") } }
            )
            CampoTexto("PC-CONT-14", {}, "Código del equipo", ayuda = "Ese equipo no está en el catálogo", error = true)
        }
    }
}

@Preview(name = "Campos, claro", showBackground = true)
@Composable
private fun CamposClaro() = SigtecTheme { CamposDePrueba() }

@Preview(name = "Campos, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CamposOscuro() = SigtecTheme(darkTheme = true) { CamposDePrueba() }
