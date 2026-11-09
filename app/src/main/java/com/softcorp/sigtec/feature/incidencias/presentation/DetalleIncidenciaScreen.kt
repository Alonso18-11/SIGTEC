package com.softcorp.sigtec.feature.incidencias.presentation

import com.softcorp.sigtec.core.ui.components.IconosBarraEstadoClaros
import com.softcorp.sigtec.core.ui.components.Tarjeta
import com.softcorp.sigtec.core.ui.components.PuntoLinea
import com.softcorp.sigtec.core.ui.components.PasosIncidencia
import com.softcorp.sigtec.core.ui.components.LineaTiempo
import com.softcorp.sigtec.core.ui.components.EventoLinea
import com.softcorp.sigtec.core.ui.components.CabeceraHero
import com.softcorp.sigtec.core.ui.components.BarraAccionInferior
import com.softcorp.sigtec.core.ui.components.AvanceIncidencia
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.AltoEtiqueta
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Evidencia
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
import com.softcorp.sigtec.feature.captura.presentation.BotonLinterna
import com.softcorp.sigtec.feature.captura.presentation.FotoAmpliada
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.estadoVisible
import com.softcorp.sigtec.feature.atencion.presentation.colorEstadoSolicitud
import com.softcorp.sigtec.feature.atencion.presentation.textoEstadoSolicitud
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado
import com.softcorp.sigtec.feature.captura.presentation.SeccionEvidencias
import com.softcorp.sigtec.feature.captura.presentation.mensajeEliminarFoto
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
    val alEliminarFoto: (Evidencia) -> Unit = {},
    val alCerrarFoto: () -> Unit = {},
    val alAbrirAsistente: () -> Unit = {},
    val alVerHistorial: (String) -> Unit = {},
    val alAlternarLinterna: () -> Unit = {}
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
    // HU-10 (RF-1026): la linterna se apaga al salir, al pasar a segundo plano o al bloquear el celular
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.alDejarDeVer() }
    LaunchedEffect(estado.errorLinterna) {
        if (estado.errorLinterna) {
            vm.alAvisoLinternaMostrado()
            avisos.showSnackbar("No se pudo encender la linterna.")   // RF-1028
        }
    }
    DetalleIncidenciaScreen(
        estado,
        acciones.copy(
            alIniciarAtencion = vm::alIniciarAtencion,
            alEliminarFoto = vm::alEliminarFoto,
            alCerrarFoto = vm::alCerrarFoto,
            alAlternarLinterna = vm::alAlternarLinterna
        ),
        avisos
    )
}

@Composable
fun DetalleIncidenciaScreen(
    estado: DetalleIncidenciaUiState,
    acciones: AccionesDetalle,
    avisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    val incidencia = estado.detalle?.incidencia
    var menuAbierto by remember { mutableStateOf(false) }
    // RF-934: guarda el id, no la foto: al eliminarla deja de estar en la lista y la vista ampliada se cierra sola
    var fotoAbiertaId by rememberSaveable { mutableStateOf<String?>(null) }

    // Pantallas v2 (prototipo 05): con la incidencia cargada, la cabecera oscura va dentro del contenido y llega detrás
    // de la barra de estado; mientras carga o si no existe, la barra superior de siempre
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(avisos) },
        topBar = { if (estado.detalle == null) BarraSuperior("Incidencia", alVolver = acciones.alVolver) },
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
                else -> Contenido(
                    estado.detalle,
                    estado.ahora,
                    puedeAgregarFoto = estado.puedeAtender,
                    acciones,
                    alAbrirFoto = { fotoAbiertaId = it.id },
                    cabecera = {
                        Cabecera(estado.detalle.incidencia, estado, acciones, menuAbierto, alMenu = { menuAbierto = it })
                    }
                )
            }
        }
    }

    estado.detalle?.evidencias?.firstOrNull { it.id == fotoAbiertaId }?.let { foto ->
        FotoAmpliada(
            foto = foto,
            alVolver = { fotoAbiertaId = null; acciones.alCerrarFoto() },
            puedeEliminar = estado.puedeAtender,   // RF-937: la misma condición que «Agregar foto»
            eliminando = estado.eliminandoFoto,
            error = estado.errorFoto?.let(::mensajeEliminarFoto),
            alEliminar = { acciones.alEliminarFoto(foto) }
        )
    }
}

// Prototipo 05: cabecera con el problema y su estado; tarjetas de equipo y usuario; avance; datos; informe,
// repuestos, evidencias y actividad.
@Composable
private fun Contenido(
    detalle: DetalleIncidencia,
    ahora: Instant,
    puedeAgregarFoto: Boolean,
    acciones: AccionesDetalle,
    alAbrirFoto: (Evidencia) -> Unit,
    cabecera: @Composable () -> Unit
) {
    val incidencia = detalle.incidencia
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        cabecera()
        Column(
            Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.l)
        ) {
            ContenidoCuerpo(detalle, ahora, puedeAgregarFoto, acciones, alAbrirFoto)
        }
    }
}

@Composable
private fun ContenidoCuerpo(
    detalle: DetalleIncidencia,
    ahora: Instant,
    puedeAgregarFoto: Boolean,
    acciones: AccionesDetalle,
    alAbrirFoto: (Evidencia) -> Unit
) {
    val incidencia = detalle.incidencia
    TarjetasEquipoUsuario(incidencia, acciones.alVerHistorial)
    // El avance tiene sentido desde que hay técnico; «Sin asignar» se lee en la etiqueta de la cabecera
    pasoActual(incidencia.marca)?.let { AvanceIncidencia(actual = it) }
    PanelDatos(incidencia)

    // ---------- Informe técnico (HU-08): solo si ya llegó a este celular (RF-827 a RF-829) ----------
    if (incidencia.estado == EstadoIncidencia.SOLUCIONADO && detalle.informe != null) {
        SeccionInforme(detalle.informe, incidencia.tecnicoAsignadoNombre ?: SIN_TECNICO)
    }

    // ---------- Repuestos (HU-11): los ve cualquier perfil (RF-1120) ----------
    if (detalle.solicitudes.isNotEmpty()) SeccionRepuestos(detalle.solicitudes, ahora)

    // ---------- Evidencias (HU-09): las ve cualquiera; «Agregar foto» solo el técnico asignado (RF-923, RF-932) ----------
    SeccionEvidencias(detalle.evidencias, puedeAgregarFoto, acciones.alAgregarFoto, alAbrirFoto)

    // ---------- Línea de tiempo: lo más reciente arriba ----------
    Seccion("Actividad") { LineaTiempo(eventosActividad(detalle.movimientos, incidencia.estado, ahora)) }
}

/** Avance de la atención: Asignada, En atención, Repuesto y Cierre; null si todavía no tiene técnico. */
private fun pasoActual(marca: MarcaSeguimiento): Int? = when (marca) {
    MarcaSeguimiento.SIN_ASIGNAR -> null
    MarcaSeguimiento.ASIGNADA -> 0
    MarcaSeguimiento.EN_ATENCION -> 1
    MarcaSeguimiento.ESPERANDO_REPUESTO -> 2
    MarcaSeguimiento.CERRADA -> PasosIncidencia.size
}

/** Cabecera oscura (prototipo 05): volver, código, linterna y menú; el problema, su estado y su clasificación. */
@Composable
private fun Cabecera(
    incidencia: Incidencia,
    estado: DetalleIncidenciaUiState,
    acciones: AccionesDetalle,
    menuAbierto: Boolean,
    alMenu: (Boolean) -> Unit
) {
    val marca = SigtecTheme.marca
    IconosBarraEstadoClaros()   // la cabecera oscura queda detrás de la barra de estado
    CabeceraHero {
        Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = acciones.alVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = marca.sobreHero)
            }
            Text(
                incidencia.codigoParaMostrar(),
                style = MaterialTheme.typography.titleMedium,
                color = marca.sobreHeroTenue,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            // HU-10 (RF-1024): antes del menú, solo para el técnico asignado
            if (estado.linternaVisible) {
                BotonLinterna(encendida = estado.linternaEncendida, alAlternar = acciones.alAlternarLinterna, enCabecera = true)
            }
            Box {
                IconButton(onClick = { alMenu(true) }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = marca.sobreHero)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { alMenu(false) }) {
                    DropdownMenuItem(
                        text = { Text("Abrir asistente") },
                        onClick = { alMenu(false); acciones.alAbrirAsistente() }
                    )
                }
            }
        }
        Text(incidencia.descripcion, style = MaterialTheme.typography.headlineMedium, color = marca.sobreHero)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            // Cerrada ya se lee «Solucionado»; si sigue pendiente, la marca dice en qué punto está
            EtiquetaIncidencia(incidencia.marca, incidencia.critica, incidencia.diasPendiente(estado.ahora))
            listOfNotNull(incidencia.tipoFalla?.etiqueta(), incidencia.prioridad?.etiqueta()).forEach { EtiquetaCabecera(it) }
        }
        if (incidencia.estado == EstadoIncidencia.PENDIENTE) {
            Text("Estado: Pendiente", style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue)
        }
    }
}

/** Clasificación sobre la cabecera oscura: translúcida, sin acento (plan de pantallas v2, P-3). */
@Composable
private fun EtiquetaCabecera(texto: String) {
    Surface(
        color = SigtecTheme.marca.sobreHero.copy(alpha = 0.14f),
        contentColor = SigtecTheme.marca.sobreHero,
        shape = FormaCompleta,
        modifier = Modifier.heightIn(min = AltoEtiqueta)
    ) {
        Box(Modifier.padding(horizontal = Espaciado.sm), contentAlignment = Alignment.Center) {
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Prototipo 05: el equipo con su historial y el usuario responsable, lado a lado. */
@Composable
private fun TarjetasEquipoUsuario(incidencia: Incidencia, alVerHistorial: (String) -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
        Tarjeta(Modifier.weight(1f).fillMaxHeight()) {
            Text("Equipo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(incidencia.codigoEquipo, style = MaterialTheme.typography.titleSmall)
            Text(
                "Ver historial",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = Espaciado.xs)
                    .heightIn(min = AreaTactilMinima)
                    .clickable(role = Role.Button) { alVerHistorial(incidencia.codigoEquipo) }
                    .wrapContentHeight()
            )
        }
        Tarjeta(Modifier.weight(1f).fillMaxHeight()) {
            Text("Usuario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(incidencia.usuarioResponsable, style = MaterialTheme.typography.titleSmall)
            incidencia.areaResponsable?.let {   // HU-12 (RF-1216)
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PanelDatos(incidencia: Incidencia) {
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            FilaDato("Técnico asignado") {
                val tecnico = incidencia.tecnicoAsignadoNombre
                if (tecnico != null) Persona(tecnico) else ValorDato(SIN_TECNICO, tenue = true)
            }
            FilaDato("Registrado por") { Persona(incidencia.registradoPorNombre) }
            FilaDato("Fecha y hora") { ValorDato(fechaCompleta(incidencia.fechaRegistro)) }
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

/** HU-11 (RF-1120): cada pieza pedida, su estado con texto y color, cuándo se pidió y quién la confirmó con huella. */
@Composable
private fun SeccionRepuestos(solicitudes: List<SolicitudRepuesto>, ahora: Instant) {
    Seccion("Repuestos") {
        Tarjeta(Modifier.fillMaxWidth()) {
            solicitudes.forEachIndexed { i, solicitud ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = Espaciado.sm), color = MaterialTheme.colorScheme.outlineVariant)
                val estado = estadoVisible(solicitud)
                Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            solicitud.pieza,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(Espaciado.s))
                        EtiquetaEstado(textoEstadoSolicitud(estado), colorEstadoSolicitud(estado))
                    }
                    Text(
                        "Pedida ${fechaCorta(solicitud.fechaSolicitud, ahora)}, confirmada con huella por " +
                            solicitud.solicitadoPorNombre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** RF-827: diagnóstico, solución aplicada, piezas (si las hay), observaciones (si las hay), y quién/cuándo confirmó. */
@Composable
private fun SeccionInforme(informe: InformeTecnico, tecnicoNombre: String) {
    Seccion("Informe técnico") {
        Tarjeta(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
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
                        "Confirmado por $tecnicoNombre, ${fechaCompleta(informe.confirmadoEn)}",
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

/**
 * Actividad con `LineaTiempo` (DESIGN.md 4): lo más reciente arriba. Mientras la incidencia sigue pendiente, el último
 * movimiento va en ámbar con halo; el cierre, en verde.
 */
private fun eventosActividad(movimientos: List<Movimiento>, estado: EstadoIncidencia, ahora: Instant): List<EventoLinea> =
    movimientos.mapIndexed { i, movimiento ->
        EventoLinea(
            titulo = movimiento.tipo.titulo(),
            detalle = "${movimiento.detalle ?: movimiento.autorNombre}, ${fechaCorta(movimiento.fecha, ahora)}",
            punto = when {
                movimiento.tipo == TipoMovimiento.CERRADA -> PuntoLinea.SOLUCIONADO
                i == 0 && estado == EstadoIncidencia.PENDIENTE -> PuntoLinea.EN_CURSO
                else -> PuntoLinea.NORMAL
            }
        )
    }

@Composable
private fun BarraAcciones(estado: DetalleIncidenciaUiState, acciones: AccionesDetalle) {
    if (!estado.puedeAsignar && !estado.puedeAtender && !estado.puedeIniciarAtencion) return
    // Pantallas v2 (prototipo 05): hoja blanca pegada abajo
    BarraAccionInferior(
        aviso = estado.errorAtencion?.let { error ->
            { Text(error.mensaje(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    ) {
            Column(
                modifier = Modifier.weight(1f),
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
                    // Retema v2 (prototipo 05): «Repuesto» más angosto y «Cerrar con informe» más ancho; con la
                    // tipografía v2, los dos textos largos no cabían en dos columnas iguales
                    Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                        BotonSecundario(onClick = acciones.alSolicitarRepuesto, modifier = Modifier.weight(1f)) {
                            IconoBoton(Icons.Outlined.Inventory2)
                            Text("Repuesto", maxLines = 1)
                        }
                        val contenidoCerrar: @Composable RowScope.() -> Unit = {
                            IconoBoton(Icons.Outlined.Check)
                            Text("Cerrar con informe", maxLines = 1)
                        }
                        if (estado.puedeIniciarAtencion) {
                            BotonSecundario(onClick = acciones.alCerrar, modifier = Modifier.weight(1.5f), content = contenidoCerrar)
                        } else {
                            BotonPrincipal(onClick = acciones.alCerrar, modifier = Modifier.weight(1.5f), content = contenidoCerrar)
                        }
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
        areaResponsable = "Contabilidad",
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

@Preview(name = "Técnico, claro", showBackground = true, heightDp = 900)
@Composable
private fun DetalleTecnico() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(
                cargando = false, detalle = detalleDePrueba, puedeAtender = true, ahora = ahoraDePrueba,
                linternaVisible = true
            ),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Jefe, por sincronizar, oscuro", showBackground = true, heightDp = 900,
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

@Preview(name = "Jefe, con técnico, claro", showBackground = true, heightDp = 900)
@Composable
private fun DetalleJefeReasignar() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = detalleDePrueba, puedeAsignar = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Jefe, con técnico, oscuro", showBackground = true, heightDp = 900,
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

@Preview(name = "Técnico, puede iniciar atención, claro", showBackground = true, heightDp = 900)
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

@Preview(name = "Cerrada con informe, oscuro", showBackground = true, heightDp = 900,
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

// HU-11 (RF-1120): una pieza ya en camino y otra que todavía no sale del celular
private val conRepuestos = detalleDePrueba.copy(
    incidencia = detalleDePrueba.incidencia.copy(marca = MarcaSeguimiento.ESPERANDO_REPUESTO),
    solicitudes = listOf(
        SolicitudRepuesto(
            id = "s2", incidenciaId = "c", pieza = "Cable HDMI 1,8 m", motivo = "El cable está pelado",
            fechaSolicitud = ahoraDePrueba.minus(Duration.ofMinutes(10)), confirmadoEn = ahoraDePrueba.minus(Duration.ofMinutes(10)),
            estadoEntrega = EstadoEntrega.POR_DESPACHAR, solicitadoPorId = "t1", solicitadoPorNombre = "Luis Ramírez"
        ),
        SolicitudRepuesto(
            id = "s1", incidenciaId = "c", pieza = "Fuente de poder ATX 500 W", motivo = "No entrega energía",
            fechaSolicitud = ahoraDePrueba.minus(Duration.ofMinutes(60)), confirmadoEn = ahoraDePrueba.minus(Duration.ofMinutes(60)),
            estadoEntrega = EstadoEntrega.EN_CAMINO, solicitadoPorId = "t1", solicitadoPorNombre = "Luis Ramírez",
            numeroLogistica = "17"
        )
    )
)

@Preview(name = "Con repuestos, claro", showBackground = true, heightDp = 1100)
@Composable
private fun DetalleConRepuestosClaro() = SigtecTheme {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = conRepuestos, puedeAtender = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}

@Preview(name = "Con repuestos, oscuro", showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetalleConRepuestosOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        DetalleIncidenciaScreen(
            DetalleIncidenciaUiState(cargando = false, detalle = conRepuestos, puedeAtender = true, ahora = ahoraDePrueba),
            AccionesDetalle()
        )
    }
}
