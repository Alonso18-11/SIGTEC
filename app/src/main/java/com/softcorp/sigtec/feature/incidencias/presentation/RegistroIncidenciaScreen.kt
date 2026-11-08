package com.softcorp.sigtec.feature.incidencias.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.core.ui.components.EncabezadoSeccion
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private const val LINEAS_DESCRIPCION = 4

/** HU-10 (RF-1003): «Nueva incidencia», con el código del equipo si ya está escrito. */
private fun subtituloDictado(codigo: String): String =
    codigo.trim().uppercase().let { if (it.isEmpty()) "Nueva incidencia" else "Nueva incidencia - $it" }

private val AnchoEtiquetaDato = 148.dp

@Composable
fun RegistroIncidenciaRoute(
    codigoEscaneado: String?,
    alConsumirCodigo: () -> Unit,
    textoDictado: String?,
    alConsumirDictado: () -> Unit,
    alVolver: () -> Unit,
    alEscanear: () -> Unit,
    alDictar: (subtitulo: String) -> Unit,
    alRegistrarse: (String) -> Unit,
    vm: RegistroIncidenciaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    // RF-910: se aplica una sola vez y se borra, así una rotación no lo vuelve a escribir
    LaunchedEffect(codigoEscaneado) {
        codigoEscaneado?.let { codigo ->
            vm.alUsarCodigoEscaneado(codigo)
            alConsumirCodigo()
        }
    }

    // HU-10 (RF-1011): igual que el código, se aplica una sola vez y se borra
    LaunchedEffect(textoDictado) {
        textoDictado?.let { texto ->
            vm.alRecibirDictado(texto)
            alConsumirDictado()
        }
    }

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
        alDictar = { alDictar(subtituloDictado(estado.codigoEquipo)) },
        alGuardar = vm::guardar,
        alUsarDelDirectorio = vm::alUsarDelDirectorio,
        alReintentarDirectorio = vm::alReintentarDirectorio
    )
}

@Composable
fun RegistroIncidenciaScreen(
    estado: RegistroIncidenciaUiState,
    alVolver: () -> Unit,
    alCambiarCodigo: (String) -> Unit,
    alCambiarDescripcion: (String) -> Unit,
    alCambiarResponsable: (String) -> Unit,
    alEscanear: () -> Unit,
    alDictar: () -> Unit,
    alGuardar: () -> Unit,
    alUsarDelDirectorio: () -> Unit = {},
    alReintentarDirectorio: () -> Unit = {}
) {
    val editable = !estado.guardando

    // La raíz ya aplica los márgenes de la barra de estado; BarraSuperior ya los trae en cero.
    // imePadding en el Scaffold: con el teclado abierto, la barra de "Guardar" queda sobre él.
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Nueva incidencia", alVolver = alVolver) },
        bottomBar = { BarraGuardar(estado, alGuardar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            EncabezadoSeccion("Datos del problema")
            // ---------- Equipo (el escaneo es la HU-09) ----------
            OutlinedTextField(
                value = estado.codigoEquipo,
                onValueChange = alCambiarCodigo,
                label = { Text("Código del equipo *") },
                placeholder = { Text("PC-CONT-014") },
                trailingIcon = {
                    IconButton(onClick = alEscanear, enabled = editable) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "Escanear etiqueta",
                            tint = if (editable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                // HU-09 (RF-910, RF-911): dice de dónde salió el código mientras nadie lo edite a mano.
                // Sin la ayuda «Escríbelo o léelo…»: el botón de cámara ya lo dice (reglas de redacción, regla 2)
                supportingText = if (estado.codigoLeidoConCamara) {
                    { Text("Leído desde la etiqueta con la cámara") }
                } else null,
                singleLine = true,
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Responsable: lo completa el directorio (HU-12) y se puede corregir ----------
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                OutlinedTextField(
                    value = estado.responsable,
                    onValueChange = alCambiarResponsable,
                    label = { Text("Usuario responsable *") },
                    // RF-1206 y RF-1208: dice de dónde salió el nombre mientras nadie lo edite
                    supportingText = {
                        Text(if (estado.responsableDelDirectorio) "Completado desde el directorio" else "Quien usa el equipo")
                    },
                    singleLine = true,
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                AvisoDirectorio(estado, editable, alUsarDelDirectorio, alReintentarDirectorio)
            }

            // ---------- Descripción (el dictado es la HU-10) ----------
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = alCambiarDescripcion,
                label = { Text("Descripción del problema *") },
                trailingIcon = {
                    IconButton(onClick = alDictar, enabled = editable) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Dictar descripción",
                            tint = if (editable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                minLines = LINEAS_DESCRIPCION,
                enabled = editable,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Datos automáticos: no se editan ----------
            EncabezadoSeccion("Datos automáticos", Modifier.padding(top = Espaciado.s))
            OutlinedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
                    verticalArrangement = Arrangement.spacedBy(Espaciado.s)
                ) {
                    DatoAutomatico(
                        etiqueta = "Fecha y hora",
                        valor = formatoFecha.format(estado.fechaHora.atZone(ZoneId.systemDefault()))
                    )
                    DatoAutomatico(etiqueta = "Registrado por", valor = estado.registradoPor ?: "—")
                }
            }
        }
    }
}

@Composable
private fun DatoAutomatico(etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(AnchoEtiquetaDato)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** HU-12: lo que respondió el directorio, bajo el campo «Usuario responsable». */
@Composable
private fun AvisoDirectorio(
    estado: RegistroIncidenciaUiState,
    editable: Boolean,
    alUsar: () -> Unit,
    alReintentar: () -> Unit
) {
    val tenue = MaterialTheme.colorScheme.onSurfaceVariant
    val sugerencia = estado.sugerenciaDirectorio
    val areaYAnexo = estado.areaYAnexo
    val margen = Modifier.padding(horizontal = Espaciado.m)
    when {
        // RF-1206: prototipo 3, «Contabilidad, anexo 2314»
        areaYAnexo != null -> Text(areaYAnexo, style = MaterialTheme.typography.bodyMedium, color = tenue, modifier = margen)

        // RF-1203
        estado.directorio == EstadoDirectorio.Buscando -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
            modifier = margen
        ) {
            CircularProgressIndicator(modifier = Modifier.size(Espaciado.m), strokeWidth = 2.dp)
            Text("Buscando en el directorio…", style = MaterialTheme.typography.bodyMedium, color = tenue)
        }

        // RF-1207: no se pisa lo escrito a mano
        sugerencia != null -> Row(verticalAlignment = Alignment.CenterVertically, modifier = margen) {
            Text(
                "El directorio indica: ${sugerencia.nombre}" + sugerencia.area.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = tenue,
                modifier = Modifier.weight(1f)
            )
            BotonTexto(onClick = alUsar, enabled = editable) { Text("Usar") }
        }

        // RF-1211
        estado.directorio == EstadoDirectorio.NoEncontrado -> Text(
            "El directorio no tiene un responsable para este equipo. Escríbelo a mano.",
            style = MaterialTheme.typography.bodyMedium,
            color = tenue,
            modifier = margen
        )

        // RF-1212
        estado.directorio == EstadoDirectorio.SinRespuesta -> Row(verticalAlignment = Alignment.CenterVertically, modifier = margen) {
            Text(
                "No se pudo consultar el directorio. Escribe el responsable a mano.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            BotonTexto(onClick = alReintentar, enabled = editable) { Text("Reintentar") }
        }
    }
}

@Composable
private fun BarraGuardar(estado: RegistroIncidenciaUiState, alGuardar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
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
                BotonPrincipal(
                    onClick = alGuardar,
                    enabled = estado.puedeGuardar,
                    modifier = Modifier.fillMaxWidth()
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
            }
        }
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val fechaDePrueba = Instant.parse("2026-09-27T14:15:00Z")

@Preview(name = "Lleno, claro", showBackground = true)
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

@Preview(name = "Vacío, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroVacio() = SigtecTheme(darkTheme = true) {
    Surface {
        RegistroIncidenciaScreen(
            RegistroIncidenciaUiState(fechaHora = fechaDePrueba, registradoPor = "Luis Ramírez"),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Código escaneado, claro", showBackground = true)
@Composable
private fun RegistroEscaneadoClaro() = SigtecTheme { Surface { RegistroEscaneado() } }

@Preview(name = "Código escaneado, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroEscaneadoOscuro() = SigtecTheme(darkTheme = true) { Surface { RegistroEscaneado() } }

// HU-09: prototipo 4, con el código recién leído de la etiqueta
@Composable
private fun RegistroEscaneado() = RegistroIncidenciaScreen(
    RegistroIncidenciaUiState(
        codigoEquipo = "PC-CONT-014",
        codigoLeidoConCamara = true,
        fechaHora = fechaDePrueba,
        registradoPor = "Luis Ramírez"
    ),
    {}, {}, {}, {}, {}, {}, {}
)

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
// ---------- HU-12: el directorio de colaboradores bajo «Usuario responsable» ----------

private val mariaDelDirectorio = Colaborador("María Quispe Huamán", "Contabilidad", "2314")

private fun conDirectorio(
    directorio: EstadoDirectorio,
    responsable: String = "",
    delDirectorio: Boolean = false
) = RegistroIncidenciaUiState(
    codigoEquipo = "PC-CONT-014",
    codigoLeidoConCamara = true,
    responsable = responsable,
    directorio = directorio,
    responsableDelDirectorio = delDirectorio,
    fechaHora = fechaDePrueba,
    registradoPor = "Luis Ramírez"
)

@Preview(name = "Completado desde el directorio, claro", showBackground = true)
@Composable
private fun RegistroDirectorioClaro() = SigtecTheme {
    Surface {
        RegistroIncidenciaScreen(
            conDirectorio(EstadoDirectorio.Encontrado(mariaDelDirectorio), "María Quispe Huamán", delDirectorio = true),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Completado desde el directorio, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroDirectorioOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        RegistroIncidenciaScreen(
            conDirectorio(EstadoDirectorio.Encontrado(mariaDelDirectorio), "María Quispe Huamán", delDirectorio = true),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Buscando en el directorio, claro", showBackground = true)
@Composable
private fun RegistroBuscando() = SigtecTheme {
    Surface { RegistroIncidenciaScreen(conDirectorio(EstadoDirectorio.Buscando), {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Sugerencia con «Usar», oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroSugerencia() = SigtecTheme(darkTheme = true) {
    Surface {
        RegistroIncidenciaScreen(
            conDirectorio(EstadoDirectorio.Encontrado(mariaDelDirectorio), "Juan Pérez"),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Sin responsable en el directorio, claro", showBackground = true)
@Composable
private fun RegistroNoEncontrado() = SigtecTheme {
    Surface { RegistroIncidenciaScreen(conDirectorio(EstadoDirectorio.NoEncontrado), {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Directorio sin respuesta, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroSinRespuesta() = SigtecTheme(darkTheme = true) {
    Surface { RegistroIncidenciaScreen(conDirectorio(EstadoDirectorio.SinRespuesta), {}, {}, {}, {}, {}, {}, {}) }
}
