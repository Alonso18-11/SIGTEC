package com.softcorp.sigtec.feature.incidencias.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.mensaje
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")

private const val LINEAS_DESCRIPCION = 4

@Composable
fun RegistroIncidenciaRoute(
    alVolver: () -> Unit,
    alEscanear: () -> Unit,
    alDictar: () -> Unit,
    alRegistrarse: (String) -> Unit,
    vm: RegistroIncidenciaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.registradaId) {
        estado.registradaId?.let { id ->
            vm.alNavegarAlDetalle()
            alRegistrarse(id)
        }
    }

    RegistroIncidenciaScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarCodigo = vm::alCambiarCodigo,
        alCambiarDescripcion = vm::alCambiarDescripcion,
        alCambiarResponsable = vm::alCambiarResponsable,
        alEscanear = alEscanear,
        alDictar = alDictar,
        alGuardar = vm::guardar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroIncidenciaScreen(
    estado: RegistroIncidenciaUiState,
    alVolver: () -> Unit,
    alCambiarCodigo: (String) -> Unit,
    alCambiarDescripcion: (String) -> Unit,
    alCambiarResponsable: (String) -> Unit,
    alEscanear: () -> Unit,
    alDictar: () -> Unit,
    alGuardar: () -> Unit
) {
    val editable = !estado.guardando

    // La raíz ya aplica los márgenes de la barra de estado: aquí van en cero para no duplicarlos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Nueva incidencia") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = { BarraGuardar(estado, alGuardar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            // ---------- Equipo (el escaneo es la HU-09) ----------
            OutlinedTextField(
                value = estado.codigoEquipo,
                onValueChange = alCambiarCodigo,
                label = { Text("Código del equipo *") },
                placeholder = { Text("PC-CONT-014") },
                trailingIcon = {
                    FilledTonalIconButton(onClick = alEscanear, enabled = editable) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Escanear etiqueta")
                    }
                },
                supportingText = { Text("Leído desde la etiqueta con la cámara") },
                singleLine = true,
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Responsable (el directorio es la HU-12) ----------
            OutlinedTextField(
                value = estado.responsable,
                onValueChange = alCambiarResponsable,
                label = { Text("Usuario responsable *") },
                supportingText = { Text("Quien usa el equipo") },
                singleLine = true,
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Descripción (el dictado es la HU-10) ----------
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = alCambiarDescripcion,
                label = { Text("Descripción del problema *") },
                trailingIcon = {
                    FilledTonalIconButton(onClick = alDictar, enabled = editable) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Dictar descripción")
                    }
                },
                supportingText = { Text("Puedes dictarla con el micrófono y corregirla antes de guardar") },
                minLines = LINEAS_DESCRIPCION,
                enabled = editable,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Datos automáticos: no se editan ----------
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.m)) {
                DatoAutomatico(
                    etiqueta = "Fecha y hora (automática)",
                    valor = formatoFecha.format(estado.fechaHora.atZone(ZoneId.systemDefault())),
                    modifier = Modifier.weight(1f)
                )
                DatoAutomatico(
                    etiqueta = "Registrado por",
                    valor = estado.registradoPor ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DatoAutomatico(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s)) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun BarraGuardar(estado: RegistroIncidenciaUiState, alGuardar: () -> Unit) {
    Column {
        HorizontalDivider()
        Column(
            modifier = Modifier.padding(Espaciado.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            estado.error?.let { error ->
                Text(
                    error.mensaje(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = alGuardar,
                enabled = estado.puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AreaTactilMinima)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Espaciado.l),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar incidencia")
                }
            }
            Text(
                "La incidencia se guardará en estado Pendiente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val fechaDePrueba = Instant.parse("2026-09-27T14:15:00Z")

@Preview(name = "Lleno · claro", showBackground = true)
@Composable
private fun RegistroLleno() = SigtecTheme {
    Surface {
        RegistroIncidenciaScreen(
            RegistroIncidenciaUiState(
                codigoEquipo = "PC-CONT-014",
                descripcion = "El equipo no enciende desde el corte de luz de esta mañana. " +
                        "No prende ninguna luz del case.",
                responsable = "María Quispe Huamán",
                fechaHora = fechaDePrueba,
                registradoPor = "Luis Ramírez"
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Vacío · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroVacio() = SigtecTheme(darkTheme = true) {
    Surface {
        RegistroIncidenciaScreen(
            RegistroIncidenciaUiState(fechaHora = fechaDePrueba, registradoPor = "Luis Ramírez"),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Error al guardar", showBackground = true)
@Composable
private fun RegistroConError() = SigtecTheme {
    Surface {
        RegistroIncidenciaScreen(
            RegistroIncidenciaUiState(
                codigoEquipo = "PC-CONT-014",
                descripcion = "No enciende",
                responsable = "María Quispe Huamán",
                fechaHora = fechaDePrueba,
                registradoPor = "Luis Ramírez",
                error = ErrorDominio.Desconocido(null)
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
