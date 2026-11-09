package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BotonPrincipal

/** Un mensaje de la explicación del permiso: ícono, título y texto. */
class Explicacion(val icono: ImageVector, val titulo: String, val texto: String)

/** Lo que dice la explicación de un permiso en cada [VistaPermiso] que no es `CONCEDIDO` (D-7). */
class TextosPermiso(
    val rotuloPermitir: String,
    val explicar: Explicacion,
    val negado: Explicacion,
    val irAAjustes: Explicacion,
    val sinRecurso: Explicacion
) {
    fun de(vista: VistaPermiso): Explicacion? = when (vista) {
        VistaPermiso.EXPLICAR -> explicar
        VistaPermiso.NEGADO -> negado
        VistaPermiso.IR_A_AJUSTES -> irAAjustes
        VistaPermiso.SIN_RECURSO -> sinRecurso
        VistaPermiso.CONCEDIDO -> null
    }
}

/** HU-09 (RF-916, RF-918 a RF-920): los textos de siempre del permiso de cámara. */
val TextosPermisoCamara = TextosPermiso(
    rotuloPermitir = "Permitir cámara",
    explicar = Explicacion(
        Icons.Outlined.PhotoCamera,
        "Permiso de cámara",
        "SIGTEC usa la cámara para leer la etiqueta del equipo y para fotografiar la falla y la pieza cambiada. " +
            "Las fotos se guardan solo en este celular."
    ),
    negado = Explicacion(
        Icons.Outlined.NoPhotography,
        "Sin permiso de cámara",
        "Sin el permiso de cámara, SIGTEC no puede leer la etiqueta ni tomar fotos."
    ),
    irAAjustes = Explicacion(
        Icons.Outlined.Settings,
        "Sin permiso de cámara",
        "Android ya no vuelve a preguntar. Concede el permiso en los ajustes de SIGTEC, en Permisos → Cámara."
    ),
    sinRecurso = Explicacion(Icons.Outlined.NoPhotography, "Sin cámara", "Este celular no tiene cámara.")
)

/**
 * Lo que se ve en lugar del contenido mientras no hay permiso o no hay el recurso (RF-916, RF-918 a RF-920,
 * RF-1019, RF-1021, RF-1022). No tiene estado: `ConPermiso` decide la [vista] y qué hace cada botón.
 * [textoNegado] es lo que la pantalla puede hacer sin el recurso, como escribir el código a mano (RF-918).
 */
@Composable
fun ExplicacionPermiso(
    vista: VistaPermiso,
    textos: TextosPermiso,
    textoNegado: String?,
    alPermitir: () -> Unit,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val explicacion = textos.de(vista) ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Espaciado.l),
        verticalArrangement = Arrangement.spacedBy(Espaciado.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            explicacion.icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TamanoIcono)
        )
        Text(explicacion.titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            explicacion.texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (vista != VistaPermiso.EXPLICAR && textoNegado != null) {
            Text(
                textoNegado,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        when (vista) {
            VistaPermiso.EXPLICAR, VistaPermiso.NEGADO ->
                BotonPrincipal(onClick = alPermitir) { Text(textos.rotuloPermitir) }
            VistaPermiso.IR_A_AJUSTES ->
                BotonPrincipal(onClick = alAbrirAjustes) { Text("Abrir ajustes") } // RF-919, RF-1022
            VistaPermiso.SIN_RECURSO, VistaPermiso.CONCEDIDO -> Unit // RF-920: no hay nada que pedir
        }
    }
}

private val TamanoIcono = 48.dp

private class VistasSinPermiso : PreviewParameterProvider<VistaPermiso> {
    override val values = sequenceOf(
        VistaPermiso.EXPLICAR,
        VistaPermiso.NEGADO,
        VistaPermiso.IR_A_AJUSTES,
        VistaPermiso.SIN_RECURSO
    )
}

@Preview(name = "Permiso, claro", showBackground = true, heightDp = 600)
@Composable
private fun ExplicacionClaro(@PreviewParameter(VistasSinPermiso::class) vista: VistaPermiso) =
    SigtecTheme { Surface { Previa(vista) } }

@Preview(name = "Permiso, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 600)
@Composable
private fun ExplicacionOscuro(@PreviewParameter(VistasSinPermiso::class) vista: VistaPermiso) =
    SigtecTheme(darkTheme = true) { Surface { Previa(vista) } }

@Composable
private fun Previa(vista: VistaPermiso) = ExplicacionPermiso(
    vista = vista,
    textos = TextosPermisoCamara,
    textoNegado = "También puedes escribir el código a mano en el formulario.",
    alPermitir = {},
    alAbrirAjustes = {}
)
