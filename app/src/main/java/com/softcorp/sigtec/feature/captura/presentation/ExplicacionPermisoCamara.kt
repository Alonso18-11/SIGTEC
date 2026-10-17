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

/**
 * Lo que se ve en lugar de la cámara mientras no hay permiso o no hay cámara (RF-916, RF-918 a RF-920).
 * No tiene estado: `ConPermisoCamara` decide la [vista] y qué hace cada botón.
 * [textoNegado] es lo que la pantalla puede hacer sin cámara, como escribir el código a mano (RF-918).
 */
@Composable
fun ExplicacionPermisoCamara(
    vista: VistaPermiso,
    textoNegado: String?,
    alPermitir: () -> Unit,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val explicacion = explicacionDe(vista) ?: return
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
                BotonPrincipal(onClick = alPermitir) { Text("Permitir cámara") }
            VistaPermiso.IR_A_AJUSTES ->
                BotonPrincipal(onClick = alAbrirAjustes) { Text("Abrir ajustes") } // RF-919
            VistaPermiso.SIN_CAMARA, VistaPermiso.CAMARA -> Unit // RF-920: no hay nada que pedir
        }
    }
}

private class Explicacion(val icono: ImageVector, val titulo: String, val texto: String)

private fun explicacionDe(vista: VistaPermiso): Explicacion? = when (vista) {
    VistaPermiso.EXPLICAR -> Explicacion(
        Icons.Outlined.PhotoCamera,
        "Permiso de cámara",
        "SIGTEC usa la cámara para leer la etiqueta del equipo y para fotografiar la falla y la pieza cambiada. " +
            "Las fotos se guardan solo en este celular."
    )
    VistaPermiso.NEGADO -> Explicacion(
        Icons.Outlined.NoPhotography,
        "Sin permiso de cámara",
        "Sin el permiso de cámara, SIGTEC no puede leer la etiqueta ni tomar fotos."
    )
    VistaPermiso.IR_A_AJUSTES -> Explicacion(
        Icons.Outlined.Settings,
        "Sin permiso de cámara",
        "Android ya no vuelve a preguntar. Concede el permiso en los ajustes de SIGTEC, en Permisos → Cámara."
    )
    VistaPermiso.SIN_CAMARA -> Explicacion(Icons.Outlined.NoPhotography, "Sin cámara", "Este celular no tiene cámara.")
    VistaPermiso.CAMARA -> null
}

private val TamanoIcono = 48.dp

private class VistasSinCamara : PreviewParameterProvider<VistaPermiso> {
    override val values = sequenceOf(
        VistaPermiso.EXPLICAR,
        VistaPermiso.NEGADO,
        VistaPermiso.IR_A_AJUSTES,
        VistaPermiso.SIN_CAMARA
    )
}

@Preview(name = "Permiso · claro", showBackground = true, heightDp = 600)
@Composable
private fun ExplicacionClaro(@PreviewParameter(VistasSinCamara::class) vista: VistaPermiso) =
    SigtecTheme { Surface { Previa(vista) } }

@Preview(name = "Permiso · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 600)
@Composable
private fun ExplicacionOscuro(@PreviewParameter(VistasSinCamara::class) vista: VistaPermiso) =
    SigtecTheme(darkTheme = true) { Surface { Previa(vista) } }

@Composable
private fun Previa(vista: VistaPermiso) = ExplicacionPermisoCamara(
    vista = vista,
    textoNegado = "También puedes escribir el código a mano en el formulario.",
    alPermitir = {},
    alAbrirAjustes = {}
)
