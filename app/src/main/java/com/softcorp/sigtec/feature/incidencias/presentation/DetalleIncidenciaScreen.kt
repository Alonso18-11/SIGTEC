package com.softcorp.sigtec.feature.incidencias.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.Avatar
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.core.ui.components.Seccion
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.incidencias.domain.DetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.SIN_TECNICO
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import java.time.Duration
import java.time.Instant

private val TamanoAvatarActividad = 24.dp
private val GrosorLinea = 2.dp
private val AnchoEtiquetaDato = 148.dp

data class AccionesDetalle(
    val alVolver: () -> Unit = {},
    val alAsignar: () -> Unit = {},
    val alIniciarAtencion: () -> Unit = {},
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
    DetalleIncidenciaScreen(estado, acciones.copy(alIniciarAtencion = vm::alIniciarAtencion), avisos)
}

@Composable
fun DetalleIncidenciaScreen(
    estado: DetalleIncidenciaUiState,
    acciones: AccionesDetalle,
    avisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    val incidencia = estado.detalle?.incidencia
    var menuAbierto by remember { mutableStateOf(false) }

    // La raíz ya aplica los márgenes de la barra de estado; BarraSuperior ya los trae en cero
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            BarraSuperior(
                titulo = incidencia?.codigoParaMostrar() ?: "Incidencia",
                alVolver = acciones.alVolver,
                acciones = {
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
                }
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

// Ordenado como la vista de un issue de Jira: resumen y estado arriba, «Detalles» en un panel
// de etiqueta → valor, y después el informe, las evidencias y la actividad.
@Composable
private fun Contenido(detalle: DetalleIncidencia, ahora: Instant, acciones: AccionesDetalle) {
    val incidencia = detalle.incidencia
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.m),
        verticalArrangement = Arrangement.spacedBy(Espaciado.l)
    ) {
        Encabezado(incidencia, ahora)
        PanelDatos(incidencia, acciones.alVerHistorial)

        // ---------- Informe técnico (HU-08): solo si ya llegó a este celular (RF-827 a RF-829) ----------
        if (incidencia.estado == EstadoIncidencia.SOLUCIONADO && detalle.informe != null) {
            SeccionInforme(detalle.informe, incidencia.tecnicoAsignadoNombre ?: SIN_TECNICO)
        }

        // ---------- Evidencias (las fotos son la HU-09) ----------
        Seccion("Evidencias") {
            BotonSecundario(onClick = acciones.alAgregarFoto) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Espaciado.s))
                Text("Agregar foto")
            }
        }

        // ---------- Línea de tiempo: lo más reciente arriba ----------
        Seccion("Actividad") { LineaDeTiempo(detalle.movimientos, ahora) }
    }
}

/** Resumen del problema, su estado (oficial y punto de la atención) y su clasificación. */
@Composable
private fun Encabezado(incidencia: Incidencia, ahora: Instant) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        Text(incidencia.descripcion, style = MaterialTheme.typography.titleLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            // Cerrada ya se lee «Solucionado»; si sigue pendiente, la marca dice en qué punto está
            EtiquetaIncidencia(incidencia.marca, incidencia.critica, incidencia.diasPendiente(ahora))
            if (incidencia.estado == EstadoIncidencia.PENDIENTE) {
                Text(
                    "Estado: Pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        clasificacion(incidencia)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun clasificacion(incidencia: Incidencia): String? =
    listOfNotNull(incidencia.tipoFalla?.etiqueta(), incidencia.prioridad?.etiqueta())
        .joinToString(" · ")
        .ifEmpty { null }

@Composable
private fun PanelDatos(incidencia: Incidencia, alVerHistorial: (String) -> Unit) {
    Seccion("Detalles") {
        OutlinedCard(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
                verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
            ) {
                FilaDato("Técnico asignado") {
                    val tecnico = incidencia.tecnicoAsignadoNombre
                    if (tecnico != null) Persona(tecnico) else ValorDato(SIN_TECNICO, tenue = true)
                }
                FilaDato("Equipo") {
                    Text(incidencia.codigoEquipo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    // Enlace en línea, como en Jira: sin el alto mínimo de un botón para no dejar un hueco
                    Text(
                        "Ver historial del equipo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = Espaciado.xs)
                            .clickable(role = Role.Button) { alVerHistorial(incidencia.codigoEquipo) }
                    )
                }
                FilaDato("Usuario responsable") { ValorDato(incidencia.usuarioResponsable) }
                FilaDato("Registrado por") { Persona(incidencia.registradoPorNombre) }
                FilaDato("Fecha y hora") { ValorDato(fechaCompleta(incidencia.fechaRegistro)) }
            }
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
            modifier = Modifier
                .width(AnchoEtiquetaDato)
                .padding(top = 2.dp)
        )
        Column(Modifier.weight(1f), content = valor)
    }
}

@Composable
private fun ValorDato(texto: String, tenue: Boolean = false) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = if (tenue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun Persona(nombre: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(nombre, tamano = TamanoAvatarActividad)
        Spacer(Modifier.width(Espaciado.s))
        ValorDato(nombre)
    }
}

/** RF-827: diagnóstico, solución aplicada, piezas (si las hay), observaciones (si las hay), y quién/cuándo confirmó. */
@Composable
private fun SeccionInforme(informe: InformeTecnico, tecnicoNombre: String) {
    Seccion("Informe técnico") {
        OutlinedCard(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
            ) {
                FilaInforme("Diagnóstico", informe.diagnostico)
                FilaInforme("Solución aplicada", informe.trabajoRealizado)
                informe.piezasCambiadas?.let { FilaInforme("Piezas cambiadas", it) }
                informe.observaciones?.let { FilaInforme("Observaciones", it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = SigtecTheme.estados.solucionado.contenido,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Espaciado.s))
                    Text(
                        "Confirmado por $tecnicoNombre · ${fechaCompleta(informe.confirmadoEn)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaInforme(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Actividad al estilo Jira: el avatar de quien hizo cada cambio, unidos por una línea vertical. */
@Composable
private fun LineaDeTiempo(movimientos: List<Movimiento>, ahora: Instant) {
    Column {
        movimientos.forEachIndexed { i, movimiento ->
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = Espaciado.sm)
                ) {
                    Avatar(movimiento.autorNombre, tamano = TamanoAvatarActividad)
                    if (i < movimientos.lastIndex) {
                        Box(
                            Modifier
                                .padding(vertical = Espaciado.xs)
                                .width(GrosorLinea)
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(Modifier.padding(bottom = Espaciado.m)) {
                    Text(
                        movimiento.tipo.titulo(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        "${movimiento.detalle ?: movimiento.autorNombre} · ${fechaCorta(movimiento.fecha, ahora)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraAcciones(estado: DetalleIncidenciaUiState, acciones: AccionesDetalle) {
    if (!estado.puedeAsignar && !estado.puedeAtender && !estado.puedeIniciarAtencion) return
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                // Asignar es del jefe
                if (estado.puedeAsignar) {
                    BotonPrincipal(onClick = acciones.alAsignar, modifier = Modifier.fillMaxWidth()) {
                        IconoBoton(Icons.Outlined.PersonAdd)
                        // RF-605 (HU-06): con técnico, la misma pantalla reasigna
                        Text(if (estado.detalle?.incidencia?.tecnicoAsignadoId != null) "Reasignar técnico" else "Asignar técnico")
                    }
                }
                // solo en la marca Asignada
                if (estado.puedeIniciarAtencion) {
                    BotonPrincipal(
                        onClick = acciones.alIniciarAtencion,
                        enabled = !estado.iniciandoAtencion,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconoBoton(Icons.Outlined.PlayArrow)
                        Text("Iniciar atención")
                    }
                }
                if (estado.puedeAtender) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                        BotonSecundario(onClick = acciones.alSolicitarRepuesto, modifier = Modifier.weight(1f)) {
                            IconoBoton(Icons.Outlined.Inventory2)
                            Text("Solicitar repuesto", maxLines = 1)
                        }
                        val contenidoCerrar: @Composable RowScope.() -> Unit = {
                            IconoBoton(Icons.Outlined.Check)
                            Text("Cerrar con informe", maxLines = 1)
                        }
                        if (estado.puedeIniciarAtencion) {
                            BotonSecundario(onClick = acciones.alCerrar, modifier = Modifier.weight(1f), content = contenidoCerrar)
                        } else {
                            BotonPrincipal(onClick = acciones.alCerrar, modifier = Modifier.weight(1f), content = contenidoCerrar)
                        }
                    }
                }
                estado.errorAtencion?.let {
                    Text(it.mensaje(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun IconoBoton(icono: ImageVector) {
    Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(Espaciado.s))
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

@Preview(name = "Jefe, con técnico · claro", showBackground = true, heightDp = 900)
@Composable
private fun DetalleJefeReasignar() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = detalleDePrueba, puedeAsignar = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Jefe, con técnico · oscuro", showBackground = true, heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleJefeReasignarOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = detalleDePrueba, puedeAsignar = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}

@Preview(name = "No encontrada", showBackground = true)
@Composable
private fun DetalleNoEncontrada() = SigtecTheme {
    Surface { DetalleIncidenciaScreen(DetalleIncidenciaUiState(cargando = false), AccionesDetalle()) }
}

// ---------- HU-08: «Iniciar atención» y la sección del informe técnico ----------

private val informeDePrueba = InformeTecnico(
    id = "inf1", incidenciaId = "c",
    diagnostico = "Fuente de poder dañada por el corte de luz",
    trabajoRealizado = "Se reemplazó la fuente de poder por una nueva",
    piezasCambiadas = "Fuente de poder ATX 500W",
    observaciones = "Se recomienda usar un estabilizador de corriente",
    fechaCierre = ahoraDePrueba,
    confirmadoEn = ahoraDePrueba
)

@Preview(name = "Técnico, puede iniciar atención · claro", showBackground = true, heightDp = 900)
@Composable
private fun DetalleIniciarAtencion() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = DetalleIncidencia(
                    detalleDePrueba.incidencia.copy(marca = MarcaSeguimiento.ASIGNADA),
                    detalleDePrueba.movimientos.drop(1)   // sin «Atención iniciada» todavía
                ),
                puedeAtender = true,
                puedeIniciarAtencion = true,
                ahora = ahoraDePrueba
            ),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Cerrada con informe · oscuro", showBackground = true, heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleCerradaConInforme() = SigtecTheme(darkTheme = true) {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(
                cargando = false,
                detalle = DetalleIncidencia(
                    detalleDePrueba.incidencia.copy(marca = MarcaSeguimiento.CERRADA),
                    detalleDePrueba.movimientos,
                    informe = informeDePrueba
                ),
                ahora = ahoraDePrueba
            ),
            AccionesDetalle()
        )
    }
}
