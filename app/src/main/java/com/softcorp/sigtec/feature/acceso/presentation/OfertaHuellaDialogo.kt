package com.softcorp.sigtec.feature.acceso.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.softcorp.sigtec.core.ui.components.BotonTexto

@Composable
fun OfertaHuellaDialogo(
    conSensor: Boolean,
    alActivar: () -> Unit,
    alRechazar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alRechazar,
        icon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
        title = {
            Text(if (conSensor) "¿Ingresar con tu huella?" else "¿Ingresar con el bloqueo de pantalla?")
        },
        text = {
            Text(
                if (conSensor) "La próxima vez entrarás en un segundo, sin escribir tu contraseña. " +
                        "Puedes cambiarlo en Ajustes de seguridad."
                else "Tu celular no tiene sensor de huella. Puedes desbloquear SIGTEC con el PIN, " +
                        "patrón o contraseña de tu celular."
            )
        },
        confirmButton = { BotonTexto(onClick = alActivar) { Text("Activar") } },
        dismissButton = { BotonTexto(onClick = alRechazar) { Text("Ahora no") } }
    )
}