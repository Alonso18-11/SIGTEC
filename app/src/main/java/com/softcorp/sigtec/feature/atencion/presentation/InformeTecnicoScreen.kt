package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.core.ui.components.CampoTexto
import com.softcorp.sigtec.core.ui.components.BarraAccionInferior
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.presentation.FotoAmpliada
import com.softcorp.sigtec.feature.captura.presentation.SeccionEvidencias

@Composable
fun InformeTecnicoRoute(
    alVolver: () -> Unit,
    alCerrar: () -> Unit,
    alAgregarFoto: () -> Unit,
    textosDictados: Map<CampoDictado, String?>,
    alConsumirDictado: (CampoDictado) -> Unit,
    alDictar: (campo: CampoDictado, subtitulo: String) -> Unit,
    vm: InformeTecnicoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()

    // HU-10 (RF-1011): cada texto se aplica una sola vez a su campo y se borra
    LaunchedEffect(textosDictados) {
        textosDictados.forEach { (campo, texto) ->
            if (texto != null) {
                vm.alRecibirDictado(campo, texto)
                alConsumirDictado(campo)
            }
        }
    }

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
            verificador.verificar(titulo = "Confirma el cierre") { resultado ->
                if (resultado is ResultadoBiometrico.Exito) vm.alConfirmarCierre(resultado.instante)
            }
        },
        alAgregarFoto = alAgregarFoto,
        // RF-1003: «Informe técnico - INC-0124» bajo el título de «Dictar»
        alDictar = { campo ->
            alDictar(campo, estado.codigoIncidencia?.let { "Informe técnico - $it" } ?: "Informe técnico")
        }
    )
}

/** HU-10 (RF-1001, RF-1002): el micrófono al final de un campo; se deshabilita junto con el campo. */
@Composable
private fun MicrofonoCampo(descripcion: String, habilitado: Boolean, alDictar: () -> Unit) {
    // Pantallas v2: el micrófono es uno de los usos permitidos del acento (DESIGN.md 3)
    FilledIconButton(
        onClick = alDictar,
        enabled = habilitado,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = SigtecTheme.marca.acento,
            contentColor = SigtecTheme.marca.sobreAcento
        )
    ) {
        Icon(Icons.Outlined.Mic, contentDescription = descripcion)
    }
}

@Composable
fun InformeTecnicoScreen(
    estado: InformeTecnicoUiState,
    alVolver: () -> Unit,
    alCambiarDiagnostico: (String) -> Unit,
    alCambiarTrabajo: (String) -> Unit,
    alCambiarPiezas: (String) -> Unit,
    alCambiarObservaciones: (String) -> Unit,
    alConfirmar: () -> Unit,
    alAgregarFoto: () -> Unit = {},
    alDictar: (CampoDictado) -> Unit = {}
) {
    val editable = !estado.guardando
    var intentoConfirmar by remember { mutableStateOf(false) }
    // RF-934: aquí la foto solo se ve; eliminarla se hace desde el detalle (RF-937)
    var fotoAbiertaId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Informe técnico", alVolver = alVolver) },
        // Pantallas v2 (prototipo 08): la confirmación va en la hoja inferior
        bottomBar = {
            BarraConfirmar(estado) {
                intentoConfirmar = true
                alConfirmar()
            }
        }
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
            CampoTexto(
                valor = estado.diagnostico,
                alCambiar = alCambiarDiagnostico,
                etiqueta = "Diagnóstico *",
                final = { MicrofonoCampo("Dictar diagnóstico", editable) { alDictar(CampoDictado.DIAGNOSTICO) } },
                ayuda = "La causa del problema",
                error = intentoConfirmar && estado.diagnostico.isBlank(),
                habilitado = editable,
                unaLinea = false,
                lineasMinimas = 2,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            CampoTexto(
                valor = estado.trabajoRealizado,
                alCambiar = alCambiarTrabajo,
                etiqueta = "Solución aplicada *",
                final = { MicrofonoCampo("Dictar solución aplicada", editable) { alDictar(CampoDictado.SOLUCION) } },
                ayuda = "Qué se hizo para resolverlo",
                error = intentoConfirmar && estado.trabajoRealizado.isBlank(),
                habilitado = editable,
                unaLinea = false,
                lineasMinimas = 2,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            CampoTexto(
                valor = estado.piezasCambiadas,
                alCambiar = alCambiarPiezas,
                etiqueta = "Piezas cambiadas",
                habilitado = editable,
                unaLinea = false,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            CampoTexto(
                valor = estado.observaciones,
                alCambiar = alCambiarObservaciones,
                etiqueta = "Observaciones",
                final = { MicrofonoCampo("Dictar observaciones", editable) { alDictar(CampoDictado.OBSERVACIONES) } },
                habilitado = editable,
                unaLinea = false,
                lineasMinimas = 2,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // HU-09 (RF-938, D-6): fotografiar la pieza cambiada sin salir del cierre
            SeccionEvidencias(
                evidencias = estado.evidencias,
                puedeAgregar = estado.puedeAgregarFoto && editable,
                alAgregarFoto = alAgregarFoto,
                alAbrirFoto = { fotoAbiertaId = it.id }
            )

        }
    }

    estado.evidencias.firstOrNull { it.id == fotoAbiertaId }?.let { foto ->
        FotoAmpliada(foto = foto, alVolver = { fotoAbiertaId = null })
    }
}

@Composable
private fun BarraConfirmar(estado: InformeTecnicoUiState, onClick: () -> Unit) {
    BarraAccionInferior(aviso = { AvisosConfirmar(estado) }) {
        BotonConfirmar(estado, onClick, Modifier.weight(1f))
    }
}

@Composable
private fun BotonConfirmar(estado: InformeTecnicoUiState, onClick: () -> Unit, modifier: Modifier) {
    BotonPrincipal(
        onClick = onClick,
        enabled = estado.puedeConfirmar,
        modifier = modifier
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
}

@Composable
private fun AvisosConfirmar(estado: InformeTecnicoUiState) {
    if (estado.disponibilidadBiometrica != DisponibilidadBiometrica.SIN_SEGURIDAD) {
        Text(
            "Se te pedirá tu huella o el bloqueo de pantalla para confirmar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

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

@Preview(name = "Vacío, claro", showBackground = true)
@Composable
private fun InformeVacioClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(InformeTecnicoUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Vacío, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InformeVacioOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(InformeTecnicoUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Completo y listo, claro", showBackground = true)
@Composable
private fun InformeCompletoClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(formularioCompleto, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Completo y listo, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InformeCompletoOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(formularioCompleto, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Sin bloqueo de pantalla, claro", showBackground = true)
@Composable
private fun InformeSinSeguridadClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(
            formularioCompleto.copy(disponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD),
            {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Sin bloqueo de pantalla, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

private val formularioConFotos = formularioCompleto.copy(
    puedeAgregarFoto = true,
    evidencias = listOf(
        Evidencia("a", "i127", TipoEvidencia.FALLA, "/no-existe/a.jpg", java.time.Instant.parse("2026-10-18T15:00:00Z")),
        Evidencia("b", "i127", TipoEvidencia.PIEZA_CAMBIADA, "/no-existe/b.jpg", java.time.Instant.parse("2026-10-18T15:05:00Z"))
    )
)

@Preview(name = "Con fotos, claro", showBackground = true, heightDp = 1100)
@Composable
private fun InformeConFotosClaro() = SigtecTheme {
    Surface {
        InformeTecnicoScreen(formularioConFotos, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Con fotos, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 1100)
@Composable
private fun InformeConFotosOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        InformeTecnicoScreen(formularioConFotos, {}, {}, {}, {}, {}, {})
    }
}
