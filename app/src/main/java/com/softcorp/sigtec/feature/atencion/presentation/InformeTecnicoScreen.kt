package com.softcorp.sigtec.feature.atencion.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.mensaje

/**
 * HU-08 (RF-813 a RF-817): formulario de informe técnico. Solo los cuatro campos del formulario y
 * el botón de confirmar; sin resumen de la incidencia arriba (decisión del usuario: `InformeTecnicoUiState`
 * de T31 no expone ningún dato de la incidencia, solo los del formulario).
 */
@Composable
fun InformeTecnicoRoute(
    alVolver: () -> Unit,
    alCerrar: () -> Unit,
    vm: InformeTecnicoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()

    // RF-821: el cierre exitoso navega hacia atrás; evento de un solo uso, lo consume la Route.
    LaunchedEffect(estado.cerrada) {
        if (estado.cerrada) alCerrar()
    }

    InformeTecnicoScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarDiagnostico = vm::alCambiarDiagnostico,
        alCambiarTrabajo = vm::alCambiarTrabajo,
        alCambiarPiezas = vm::alCambiarPiezas,
        alCambiarObservaciones = vm::alCambiarObservaciones,
        alConfirmar = {
            // RF-815: se pide la huella (o PIN/patrón) antes de guardar cualquier cambio.
            verificador.verificar(titulo = "Confirma el cierre") { resultado ->
                // RF-817: si se cancela o falla, no se llama al ViewModel; el formulario queda igual.
                if (resultado is ResultadoBiometrico.Exito) vm.alConfirmarCierre(resultado.instante)
            }
        }
    )
}

@Composable
fun InformeTecnicoScreen(
    estado: InformeTecnicoUiState,
    alVolver: () -> Unit,
    alCambiarDiagnostico: (String) -> Unit,
    alCambiarTrabajo: (String) -> Unit,
    alCambiarPiezas: (String) -> Unit,
    alCambiarObservaciones: (String) -> Unit,
    alConfirmar: () -> Unit
) {
    val editable = !estado.guardando
    // Solo marca los campos obligatorios como inválidos después de un intento de confirmar.
    var intentoConfirmar by remember { mutableStateOf(false) }

    // La raíz ya aplica los márgenes de la barra de estado; BarraSuperior ya los trae en cero.
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Informe técnico", alVolver = alVolver) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            Text(
                "Describe lo que encontraste y lo que hiciste. Los campos con * son obligatorios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = estado.diagnostico,
                onValueChange = alCambiarDiagnostico,
                label = { Text("Diagnóstico *") },
                supportingText = { Text("La causa del problema") },
                isError = intentoConfirmar && estado.diagnostico.isBlank(),
                enabled = editable,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado.trabajoRealizado,
                onValueChange = alCambiarTrabajo,
                label = { Text("Solución aplicada *") },
                supportingText = { Text("Qué se hizo para resolverlo") },
                isError = intentoConfirmar && estado.trabajoRealizado.isBlank(),
                enabled = editable,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado.piezasCambiadas,
                onValueChange = alCambiarPiezas,
                label = { Text("Piezas cambiadas") },
                enabled = editable,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado.observaciones,
                onValueChange = alCambiarObservaciones,
                label = { Text("Observaciones") },
                enabled = editable,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            BotonConfirmar(
                estado = estado,
                onClick = {
                    intentoConfirmar = true
                    alConfirmar()
                }
            )
        }
    }
}

@Composable
private fun BotonConfirmar(estado: InformeTecnicoUiState, onClick: () -> Unit) {
    BotonPrincipal(
        onClick = onClick,
        enabled = estado.puedeConfirmar,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (estado.guardando) {
            CircularProgressIndicator(
                modifier = Modifier.size(Espaciado.l),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Espaciado.s))
            Text("Confirmar cierre")
        }
    }
    // RF-815: se avisa antes de tocar que el cierre pide la huella o el PIN/patrón del celular
    if (estado.disponibilidadBiometrica != DisponibilidadBiometrica.SIN_SEGURIDAD) {
        Text(
            "Se te pedirá tu huella o el bloqueo de pantalla para confirmar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    // RF-816: sin bloqueo de pantalla configurado no se puede confirmar ningún cierre.
    if (estado.disponibilidadBiometrica == DisponibilidadBiometrica.SIN_SEGURIDAD) {
        Text(
            "Configura un bloqueo de pantalla en tu celular para poder confirmar el cierre.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    estado.error?.let { error ->
        Text(
            error.mensaje(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val formularioCompleto = InformeTecnicoUiState(
    diagnostico = "Fuente de poder dañada",
    trabajoRealizado = "Se reemplazó la fuente de poder",
    piezasCambiadas = "Fuente de poder 500W",
    observaciones = "Se recomienda usar un estabilizador de corriente",
    disponibilidadBiometrica = DisponibilidadBiometrica.HUELLA
)

@Preview(name = "Vacío · claro", showBackground = true)
@Composable
private fun InformeVacioClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(InformeTecnicoUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Vacío · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InformeVacioOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(InformeTecnicoUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Completo y listo · claro", showBackground = true)
@Composable
private fun InformeCompletoClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(formularioCompleto, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Completo y listo · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InformeCompletoOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(formularioCompleto, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Sin bloqueo de pantalla · claro", showBackground = true)
@Composable
private fun InformeSinSeguridadClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(
            formularioCompleto.copy(disponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD),
            {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Sin bloqueo de pantalla · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InformeSinSeguridadOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(
            formularioCompleto.copy(disponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD),
            {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Con error al guardar", showBackground = true)
@Composable
private fun InformeConError() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(
            formularioCompleto.copy(error = ErrorDominio.MarcaCambiada),
            {}, {}, {}, {}, {}, {}
        )
    }
}
