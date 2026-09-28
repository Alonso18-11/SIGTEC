package com.softcorp.sigtec.feature.incidencias.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.DetalleIncidencia
import java.time.Duration
import java.time.Instant

private val TamanoPunto = 12.dp
private val GrosorLinea = 2.dp
private val AnchoEtiquetaDato = 120.dp

/** Lo que el detalle puede abrir; cada acción navega a su pantalla (las de otras HU, provisionales). */
data class AccionesDetalle(
    val alVolver: () -> Unit = {},
    val alAsignar: () -> Unit = {},
    val alSolicitarRepuesto: () -> Unit = {},
    val alCerrar: () -> Unit = {},
    val alAgregarFoto: () -> Unit = {},
    val alAbrirAsistente: () -> Unit = {},
    val alVerHistorial: (String) -> Unit = {}
)

@Composable
fun DetalleIncidenciaRoute(
    acciones: AccionesDetalle,
    aviso: String? = null,
    vm: DetalleIncidenciaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    // El aviso que deja el registro (HU-03) se muestra una sola vez
    LaunchedEffect(aviso) { aviso?.let { avisos.showSnackbar(it) } }
    DetalleIncidenciaScreen(estado, acciones, avisos)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleIncidenciaScreen(
    estado: DetalleIncidenciaUiState,
    acciones: AccionesDetalle,
    avisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    val incidencia = estado.detalle?.incidencia
    var menuAbierto by remember { mutableStateOf(false) }

    // La raíz ya aplica los márgenes de la barra de estado: aquí van en cero para no duplicarlos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = { Text(incidencia?.codigoParaMostrar() ?: "Incidencia") },
                navigationIcon = {
                    IconButton(onClick = acciones.alVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (incidencia != null) {
                        Box {
                            IconButton(onClick = { menuAbierto = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                                DropdownMenuItem(
                                    text = { Text("Abrir asistente") },
                                    onClick = { menuAbierto = false; acciones.alAbrirAsistente() }
                                )
                            }
                        }
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = { if (incidencia != null) BarraAcciones(estado, acciones) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                estado.detalle == null -> Text(
                    "No se encontró la incidencia en este celular.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Espaciado.m)
                )
                else -> Contenido(estado.detalle, estado.ahora, acciones)
            }
        }
    }
}

@Composable
private fun Contenido(detalle: DetalleIncidencia, ahora: Instant, acciones: AccionesDetalle) {
    val incidencia = detalle.incidencia
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.m),
        verticalArrangement = Arrangement.spacedBy(Espaciado.m)
    ) {
        // ---------- Estado oficial y punto de la atención ----------
        Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            EtiquetaEstado(
                texto = if (incidencia.estado == EstadoIncidencia.SOLUCIONADO) "Solucionado" else "Pendiente",
                color = ColorEstado(
                    contenido = MaterialTheme.colorScheme.onSurfaceVariant,
                    contenedor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            // Si ya está solucionada, la primera etiqueta basta
            if (incidencia.estado == EstadoIncidencia.PENDIENTE) {
                EtiquetaIncidencia(incidencia.marca, incidencia.critica, incidencia.diasPendiente(ahora))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Text(incidencia.descripcion, style = MaterialTheme.typography.titleLarge)
            clasificacion(incidencia)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        TarjetaDatos(incidencia, acciones.alVerHistorial)

        // ---------- Evidencias (las fotos son la HU-09) ----------
        Text("Evidencias", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = acciones.alAgregarFoto) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(Espaciado.s))
            Text("Agregar foto")
        }

        // ---------- Línea de tiempo: lo más reciente arriba ----------
        Text("Movimientos", style = MaterialTheme.typography.titleMedium)
        LineaDeTiempo(detalle.movimientos, ahora)
    }
}

private fun clasificacion(incidencia: Incidencia): String? =
    listOfNotNull(incidencia.tipoFalla?.etiqueta(), incidencia.prioridad?.etiqueta())
        .joinToString(" · ")
        .ifEmpty { null }

@Composable
private fun TarjetaDatos(incidencia: Incidencia, alVerHistorial: (String) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            FilaDato("Equipo") {
                Text(incidencia.codigoEquipo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { alVerHistorial(incidencia.codigoEquipo) },
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Ver historial del equipo") }
            }
            FilaDato("Usuario responsable") { ValorDato(incidencia.usuarioResponsable) }
            FilaDato("Registrado por") { ValorDato(incidencia.registradoPorNombre) }
            FilaDato("Fecha y hora") { ValorDato(fechaCompleta(incidencia.fechaRegistro)) }
            FilaDato("Técnico asignado") { ValorDato(incidencia.tecnicoAsignadoNombre ?: SIN_TECNICO) }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: @Composable ColumnScope.() -> Unit) {
    Row {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(AnchoEtiquetaDato)
        )
        Column(Modifier.weight(1f), content = valor)
    }
}

@Composable
private fun ValorDato(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun LineaDeTiempo(movimientos: List<Movimiento>, ahora: Instant) {
    Column {
        movimientos.forEachIndexed { i, movimiento ->
            Row(Modifier.height(IntrinsicSize.Min)) {
                // Punto y línea que une con el movimiento anterior
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = Espaciado.m)
                ) {
                    Box(
                        Modifier
                            .padding(top = Espaciado.xs)
                            .size(TamanoPunto)
                            .background(
                                if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    )
                    if (i < movimientos.lastIndex) {
                        Box(
                            Modifier
                                .width(GrosorLinea)
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(Modifier.padding(bottom = Espaciado.m)) {
                    Text(movimiento.tipo.titulo(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${movimiento.detalle ?: movimiento.autorNombre} · ${fechaCorta(movimiento.fecha, ahora)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraAcciones(estado: DetalleIncidenciaUiState, acciones: AccionesDetalle) {
    if (!estado.puedeAsignar && !estado.puedeAtender) return
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier.padding(Espaciado.m),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            // Asignar es del jefe (HU-06); repuesto y cierre, de quien atiende (HU-11 y HU-08)
            if (estado.puedeAsignar) {
                Button(onClick = acciones.alAsignar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Asignar técnico")
                }
            }
            if (estado.puedeAtender) {
                OutlinedButton(onClick = acciones.alSolicitarRepuesto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null)
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Solicitar repuesto")
                }
                Button(onClick = acciones.alCerrar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Cerrar con informe")
                }
            }
        }
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val ahoraDePrueba = Instant.parse("2026-09-27T15:00:00Z")

private val detalleDePrueba = DetalleIncidencia(
    incidencia = Incidencia(
        id = "c", numero = 124, codigoEquipo = "PC-CONT-014",
        descripcion = "No enciende después del corte de luz", usuarioResponsable = "María Quispe Huamán",
        fechaRegistro = ahoraDePrueba.minus(Duration.ofMinutes(105)), registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION,
        tipoFalla = TipoFalla.HARDWARE, prioridad = Prioridad.ALTA,
        tecnicoAsignadoId = "t1", tecnicoAsignadoNombre = "Luis Ramírez"
    ),
    movimientos = listOf(
        Movimiento("m3", "c", TipoMovimiento.ATENCION_INICIADA, "Luis Ramírez", ahoraDePrueba.minus(Duration.ofMinutes(70))),
        Movimiento("m2", "c", TipoMovimiento.ASIGNADA, "Carlos Mendoza", ahoraDePrueba.minus(Duration.ofMinutes(88)),
            detalle = "Por Carlos Mendoza, jefe del área"),
        Movimiento("m1", "c", TipoMovimiento.REGISTRADA, "Luis Ramírez", ahoraDePrueba.minus(Duration.ofMinutes(105)))
    )
)

@Preview(name = "Técnico · claro", showBackground = true, heightDp = 900)
@Composable
private fun DetalleTecnico() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = detalleDePrueba, puedeAtender = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Jefe, por sincronizar · oscuro", showBackground = true, heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleJefe() = SigtecTheme(darkTheme = true) {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = DetalleIncidencia(
                    detalleDePrueba.incidencia.copy(
                        numero = null, marca = MarcaSeguimiento.SIN_ASIGNAR, tipoFalla = null, prioridad = null,
                        tecnicoAsignadoId = null, tecnicoAsignadoNombre = null
                    ),
                    detalleDePrueba.movimientos.takeLast(1)
                ),
                puedeAsignar = true,
                ahora = ahoraDePrueba
            ),
            AccionesDetalle()
        )
    }
}

@Preview(name = "No encontrada", showBackground = true)
@Composable
private fun DetalleNoEncontrada() = SigtecTheme {
    Surface { DetalleIncidenciaScreen(DetalleIncidenciaUiState(cargando = false), AccionesDetalle()) }
}
