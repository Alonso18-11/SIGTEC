package com.softcorp.sigtec.feature.asignacion.presentation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.asignacion.domain.OpcionTecnico
import com.softcorp.sigtec.feature.asignacion.domain.OpcionesAsignacion
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import com.softcorp.sigtec.feature.incidencias.presentation.diasPendiente
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta
import java.time.Instant

private val TamanoAvatar = 40.dp
private val GrosorBorde = 1.dp
private val GrosorBordeElegido = 2.dp

// RF-606
private const val SIN_TECNICOS = "Aún no se descargaron los técnicos. Conéctate a internet para verlos."

@Composable
fun AsignacionRoute(
    alVolver: () -> Unit,
    alAsignar: (String) -> Unit,
    vm: AsignacionViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    // RF-617: vuelve al detalle con el nombre del técnico para el aviso
    LaunchedEffect(estado.asignadaA) {
        estado.asignadaA?.let { nombre ->
            vm.alNavegarAlDetalle()
            alAsignar(nombre)
        }
    }

    AsignacionScreen(
        estado = estado,
        alVolver = alVolver,
        alElegir = vm::alElegir,
        alAsignar = vm::alAsignar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionScreen(
    estado: AsignacionUiState,
    alVolver: () -> Unit,
    alElegir: (String) -> Unit,
    alAsignar: () -> Unit
) {
    // La raíz ya aplica los márgenes de la barra de estado: aquí van en cero para no duplicarlos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Asignar incidencia") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = { if (estado.opciones != null) BarraAsignar(estado, alAsignar) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val opciones = estado.opciones
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                opciones == null -> Text(
                    "No se encontró la incidencia en este celular.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Espaciado.m)
                )
                else -> Contenido(opciones, estado, alElegir)
            }
        }
    }
}

@Composable
private fun Contenido(opciones: OpcionesAsignacion, estado: AsignacionUiState, alElegir: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(Espaciado.m),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        item { TarjetaResumen(opciones, estado.ahora) }
        // RF-607: el motivo se ve desde que se abre, antes de pulsar nada
        opciones.bloqueo?.let { motivo ->
            item {
                Text(motivo.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        item { Encabezado() }
        if (opciones.tecnicos.isEmpty()) {
            item {
                Text(
                    SIN_TECNICOS,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(opciones.tecnicos, key = { it.tecnico.id }) { opcion ->
            TarjetaTecnico(
                opcion = opcion,
                elegida = estado.elegido?.tecnico?.id == opcion.tecnico.id,
                habilitada = opciones.bloqueo == null && opcion.disponible && !opcion.esActual,
                alElegir = { alElegir(opcion.tecnico.id) }
            )
        }
    }
}

@Composable
private fun TarjetaResumen(opciones: OpcionesAsignacion, ahora: Instant) {
    val incidencia = opciones.incidencia
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            // «INC-0127 · IMP-RRHH-002 · Recursos Humanos»; la oficina se omite si el equipo no está en el catálogo
            Text(
                listOfNotNull(incidencia.codigoParaMostrar(), incidencia.codigoEquipo, opciones.oficina).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(incidencia.descripcion, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                EtiquetaIncidencia(incidencia.marca, incidencia.critica, incidencia.diasPendiente(ahora))
                // Sin la sugerencia de IA del prototipo: es de la HU-14
                val clasificacion = listOfNotNull(incidencia.tipoFalla?.etiqueta(), incidencia.prioridad?.etiqueta())
                if (clasificacion.isNotEmpty()) {
                    Text(
                        clasificacion.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Encabezado() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Espaciado.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Elige un técnico", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            "Incidencias activas / límite",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** RF-601 a RF-604: carga y límite con su barra; si no se puede elegir, va atenuada y dice por qué. */
@Composable
private fun TarjetaTecnico(opcion: OpcionTecnico, elegida: Boolean, habilitada: Boolean, alElegir: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tecnico = opcion.tecnico
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            elegida -> colores.primaryContainer
            habilitada -> colores.surface
            else -> colores.surfaceVariant
        },
        contentColor = when {
            elegida -> colores.onPrimaryContainer
            habilitada -> colores.onSurface
            else -> colores.onSurfaceVariant
        },
        border = BorderStroke(
            if (elegida) GrosorBordeElegido else GrosorBorde,
            if (elegida) colores.primary else colores.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = elegida, enabled = habilitada, role = Role.RadioButton, onClick = alElegir)
    ) {
        Row(
            modifier = Modifier
                .padding(Espaciado.m)
                .heightIn(min = AreaTactilMinima),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(TamanoAvatar)
                    .clip(CircleShape)
                    .background(colores.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales(tecnico.nombre), style = MaterialTheme.typography.labelLarge, color = colores.onSecondaryContainer)
            }
            Spacer(Modifier.width(Espaciado.m))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tecnico.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${tecnico.incidenciasActivas} / ${tecnico.limiteAtencion}", style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { tecnico.cargaRelativa },
                    color = if (opcion.disponible) colores.primary else colores.error,
                    trackColor = colores.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                )
                EstadoTecnico(opcion)
            }
            Spacer(Modifier.width(Espaciado.s))
            RadioButton(selected = elegida, onClick = null, enabled = habilitada)
        }
    }
}

/** Nunca solo color: siempre texto, y el candado cuando alcanzó su límite (RF-603). */
@Composable
private fun EstadoTecnico(opcion: OpcionTecnico) {
    val colores = MaterialTheme.colorScheme
    when {
        opcion.esActual -> Text("Asignado ahora", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        !opcion.disponible -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colores.error)
            Spacer(Modifier.width(Espaciado.xs))
            Text("Alcanzó su límite de atención", style = MaterialTheme.typography.bodySmall, color = colores.error)
        }
        else -> Text("Disponible", style = MaterialTheme.typography.bodySmall, color = colores.primary)
    }
}

@Composable
private fun BarraAsignar(estado: AsignacionUiState, alAsignar: () -> Unit) {
    val nombre = estado.elegido?.tecnico?.nombre
    Column {
        HorizontalDivider()
        Column(
            modifier = Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deshabilitado mientras guarda: un segundo toque no repite la asignación
            Button(
                onClick = alAsignar,
                enabled = estado.puedeAsignar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AreaTactilMinima)
            ) {
                Text(textoBotonAsignar(nombre, estado.esReasignacion))
            }
            nombre?.let {
                Text(textoBandeja(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            estado.error?.let {
                Text(it.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------- Vistas previas ----------

private val ahoraDePrueba = Instant.parse("2026-10-11T15:00:00Z")

// INC-0127 del prototipo 06
private val incidenciaDePrueba = Incidencia(
    id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
    usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
    registradoPorId = "p1", registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.SIN_ASIGNAR,
    tipoFalla = TipoFalla.RED, prioridad = Prioridad.MEDIA
)

private fun opcion(id: String, nombre: String, activas: Int, limite: Int, esActual: Boolean = false) =
    Tecnico(id, nombre, limite, activas).let { OpcionTecnico(it, disponible = it.disponible, esActual = esActual) }

private val tecnicosDePrueba = listOf(
    opcion("u-luis", "Luis Ramírez", 3, 5),
    opcion("u-ana", "Ana Torres", 5, 5),
    opcion("u-jorge", "Jorge Salas", 1, 4),
    opcion("u-rosa", "Rosa Vilca", 2, 4)
)

private fun estadoDePrueba(
    incidencia: Incidencia = incidenciaDePrueba,
    tecnicos: List<OpcionTecnico> = tecnicosDePrueba,
    bloqueo: ErrorDominio? = null,
    elegidoId: String? = "u-luis"
) = AsignacionUiState(
    cargando = false,
    opciones = OpcionesAsignacion(incidencia, "Recursos Humanos", tecnicos, bloqueo),
    elegidoId = elegidoId,
    ahora = ahoraDePrueba
)

@Composable
private fun Previa(estado: AsignacionUiState) = AsignacionScreen(estado, alVolver = {}, alElegir = {}, alAsignar = {})

@Preview(name = "Elegido · claro", showBackground = true)
@Composable
private fun AsignacionElegidoClaro() = SigtecTheme { Previa(estadoDePrueba()) }

@Preview(name = "Elegido · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsignacionElegidoOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoDePrueba()) }

@Preview(name = "Reasignar · claro", showBackground = true)
@Composable
private fun AsignacionReasignar() = SigtecTheme {
    Previa(
        estadoDePrueba(
            incidencia = incidenciaDePrueba.copy(
                marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "u-jorge", tecnicoAsignadoNombre = "Jorge Salas"
            ),
            tecnicos = tecnicosDePrueba.map { if (it.tecnico.id == "u-jorge") it.copy(esActual = true) else it },
            elegidoId = "u-rosa"
        )
    )
}

@Preview(name = "Por sincronizar · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsignacionPorSincronizar() = SigtecTheme(darkTheme = true) {
    Previa(
        estadoDePrueba(
            incidencia = incidenciaDePrueba.copy(numero = null),
            bloqueo = ErrorDominio.IncidenciaPorSincronizar,
            elegidoId = null
        )
    )
}

@Preview(name = "Sin técnicos · claro", showBackground = true)
@Composable
private fun AsignacionSinTecnicos() = SigtecTheme { Previa(estadoDePrueba(tecnicos = emptyList(), elegidoId = null)) }
