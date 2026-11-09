package com.softcorp.sigtec.feature.asignacion.presentation

import com.softcorp.sigtec.core.ui.components.Tarjeta
import com.softcorp.sigtec.core.ui.components.FilaCargaTecnico
import com.softcorp.sigtec.core.ui.components.BarraAccionInferior
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.EncabezadoSeccion
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.asignacion.domain.OpcionTecnico
import com.softcorp.sigtec.feature.asignacion.domain.OpcionesAsignacion
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import com.softcorp.sigtec.feature.incidencias.presentation.diasPendiente
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta
import java.time.Instant

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

@Composable
fun AsignacionScreen(
    estado: AsignacionUiState,
    alVolver: () -> Unit,
    alElegir: (String) -> Unit,
    alAsignar: () -> Unit
) {
    // BarraSuperior aplica el margen de la barra de estado
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Asignar incidencia", alVolver = alVolver) },
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
        // RF-601 a RF-604 con FilaCargaTecnico (prototipo 07): en el límite queda bloqueada con candado y su motivo
        items(opciones.tecnicos, key = { it.tecnico.id }) { opcion ->
            val tecnico = opcion.tecnico
            FilaCargaTecnico(
                nombre = tecnico.nombre,
                activas = tecnico.incidenciasActivas,
                limite = tecnico.limiteAtencion,
                seleccionada = estado.elegido?.tecnico?.id == tecnico.id,
                alElegir = { alElegir(tecnico.id) },
                recomendado = estado.recomendadoId == tecnico.id,
                habilitada = opciones.bloqueo == null && !opcion.esActual,
                nota = if (opcion.esActual) "Asignado ahora" else null
            )
        }
    }
}

@Composable
private fun TarjetaResumen(opciones: OpcionesAsignacion, ahora: Instant) {
    val incidencia = opciones.incidencia
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            // «INC-0127 - IMP-RRHH-002, Recursos Humanos»; la oficina se omite si el equipo no está en el catálogo
            Text(
                "${incidencia.codigoParaMostrar()} - ${incidencia.codigoEquipo}" + opciones.oficina?.let { ", $it" }.orEmpty(),
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
                        clasificacion.joinToString(", "),
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
        EncabezadoSeccion("Elige un técnico", Modifier.weight(1f))
        Text(
            "Activas / límite",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BarraAsignar(estado: AsignacionUiState, alAsignar: () -> Unit) {
    val nombre = estado.elegido?.tecnico?.nombre
    // Pantallas v2 (prototipo 07): hoja blanca abajo; el error o la bandeja van encima del botón
    val aviso: (@Composable () -> Unit)? = when {
        estado.error != null -> {
            { Text(estado.error.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
        nombre != null -> {
            { Text(textoBandeja(nombre), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        else -> null
    }
    BarraAccionInferior(aviso = aviso) {
        // Deshabilitado mientras guarda: un segundo toque no repite la asignación
        BotonPrincipal(
            onClick = alAsignar,
            enabled = estado.puedeAsignar,
            modifier = Modifier.weight(1f)
        ) {
            Text(textoBotonAsignar(nombre, estado.esReasignacion))
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

@Preview(name = "Elegido, claro", showBackground = true)
@Composable
private fun AsignacionElegidoClaro() = SigtecTheme { Previa(estadoDePrueba()) }

@Preview(name = "Elegido, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsignacionElegidoOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoDePrueba()) }

@Preview(name = "Reasignar, claro", showBackground = true)
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

@Preview(name = "Por sincronizar, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

@Preview(name = "Sin técnicos, claro", showBackground = true)
@Composable
private fun AsignacionSinTecnicos() = SigtecTheme { Previa(estadoDePrueba(tecnicos = emptyList(), elegidoId = null)) }
