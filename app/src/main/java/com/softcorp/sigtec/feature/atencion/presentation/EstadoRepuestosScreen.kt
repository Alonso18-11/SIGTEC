package com.softcorp.sigtec.feature.atencion.presentation

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import com.softcorp.sigtec.feature.atencion.domain.SolicitudConIncidencia
import com.softcorp.sigtec.feature.atencion.domain.diasDeEspera
import com.softcorp.sigtec.feature.atencion.domain.estadoVisible
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import com.softcorp.sigtec.feature.incidencias.presentation.fechaCorta
import java.time.Duration
import java.time.Instant

@Composable
fun EstadoRepuestosRoute(
    alVolver: () -> Unit,
    alAbrirIncidencia: (String) -> Unit,
    vm: EstadoRepuestosViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    EstadoRepuestosScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarFiltro = vm::alCambiarFiltro,
        alConsultar = vm::alConsultar,
        alAbrirIncidencia = alAbrirIncidencia
    )
}

/** HU-11 (prototipo 17): las piezas pedidas a Logística, con su avance. Solo dibuja. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoRepuestosScreen(
    estado: EstadoRepuestosUiState,
    alVolver: () -> Unit,
    alCambiarFiltro: (FiltroRepuestos) -> Unit,
    alConsultar: () -> Unit,
    alAbrirIncidencia: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Estado de repuestos", alVolver = alVolver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // BarraSuperior no tiene subtítulo: la hora de la última consulta va debajo (RF-1123)
            Text(
                estado.ultimaConsulta?.let { "Consultado a Logística: ${fechaCorta(it, estado.ahora)}" }
                    ?: "Todavía no se consultó a Logística",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s)
            )
            Filtros(estado.filtro, alCambiarFiltro)
            if (estado.sinRespuesta) AvisoSinRespuesta()

            // RF-1123: deslizar hacia abajo consulta a Logística
            PullToRefreshBox(
                isRefreshing = estado.consultando,
                onRefresh = alConsultar,
                modifier = Modifier.weight(1f)
            ) {
                if (!estado.cargando && estado.solicitudes.isEmpty()) {
                    SinSolicitudes(estado.filtro)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(Espaciado.m),
                        verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
                    ) {
                        items(estado.solicitudes, key = { it.solicitud.id }) { item ->
                            TarjetaSolicitud(item, estado.ahora) { alAbrirIncidencia(item.solicitud.incidenciaId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Filtros(actual: FiltroRepuestos, alCambiar: (FiltroRepuestos) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = Espaciado.m),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        FiltroRepuestos.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == actual,
                onClick = { alCambiar(filtro) },
                label = { Text(textoFiltro(filtro)) }
            )
        }
    }
}

private fun textoFiltro(filtro: FiltroRepuestos) = when (filtro) {
    FiltroRepuestos.TODOS -> "Todos"
    FiltroRepuestos.PENDIENTES -> "Pendientes"
    FiltroRepuestos.ENTREGADOS -> "Entregados"
}

@Composable
private fun AvisoSinRespuesta() {
    Row(
        modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TamanoIcono))
        Text(
            "No se pudo consultar a Logística. Se muestra lo último guardado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** RF-1122: pieza, incidencia y equipo, estado, los tres pasos y los días de espera. */
@Composable
private fun TarjetaSolicitud(item: SolicitudConIncidencia, ahora: Instant, alAbrir: () -> Unit) {
    val solicitud = item.solicitud
    val estado = estadoVisible(solicitud)
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alAbrir)
    ) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(solicitud.pieza, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${item.codigoIncidencia ?: POR_SINCRONIZAR} - ${item.codigoEquipo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(Espaciado.s))
                EtiquetaEstado(textoEstadoSolicitud(estado), colorEstadoSolicitud(estado))
            }
            Pasos(solicitud, ahora)
            Text(
                pie(solicitud, estado, ahora),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Solicitado → Despachado → Entregado; cada paso dice si ya pasó con el ícono y el texto, no solo con el color. */
@Composable
private fun Pasos(solicitud: SolicitudRepuesto, ahora: Instant) {
    val despachada = solicitud.estadoEntrega != EstadoEntrega.POR_DESPACHAR || solicitud.despachadaEn != null
    val entregada = solicitud.estadoEntrega == EstadoEntrega.ENTREGADO
    Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        Paso("Solicitado", fechaCorta(solicitud.fechaSolicitud, ahora), hecho = true, Modifier.weight(1f))
        Paso(
            "Despachado",
            solicitud.despachadaEn?.let { fechaCorta(it, ahora) }
                ?: solicitud.diasEnvio?.takeIf { !despachada }?.let { "Estimado: ${if (it == 1) "1 día" else "$it días"}" }
                ?: "—",
            hecho = despachada,
            Modifier.weight(1f)
        )
        Paso(
            "Entregado",
            solicitud.entregadaEn?.let { fechaCorta(it, ahora) }
                ?: solicitud.llegadaEstimada?.let { "Llega ${fechaCorta(it, ahora)}" }
                ?: "—",
            hecho = entregada,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun Paso(titulo: String, detalle: String, hecho: Boolean, modifier: Modifier = Modifier) {
    val color: Color = if (hecho) SigtecTheme.estados.solucionado.contenido else MaterialTheme.colorScheme.outline
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
        Icon(
            if (hecho) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (hecho) "$titulo: hecho" else "$titulo: pendiente",
            tint = color,
            modifier = Modifier.size(TamanoIcono)
        )
        Text(titulo, style = MaterialTheme.typography.labelMedium)
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun pie(solicitud: SolicitudRepuesto, estado: EstadoSolicitudVisible, ahora: Instant): String = when (estado) {
    EstadoSolicitudVisible.POR_ENVIAR -> "Se registrará en Logística en cuanto haya conexión"   // RF-1117
    EstadoSolicitudVisible.RECHAZADA -> "Logística rechazó la solicitud"                         // RF-1119
    EstadoSolicitudVisible.ENTREGADA -> "Confirmado con huella por ${solicitud.solicitadoPorNombre}"
    else -> textoDiasEspera(diasDeEspera(solicitud, ahora))                                      // RF-1122, D-9
}

@Composable
private fun SinSolicitudes(filtro: FiltroRepuestos) {
    // Dentro de una columna desplazable para que deslizar hacia abajo funcione también sin solicitudes
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Espaciado.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Text(
                    when (filtro) {
                        FiltroRepuestos.TODOS -> "No has pedido repuestos"
                        FiltroRepuestos.PENDIENTES -> "No tienes repuestos por llegar"
                        FiltroRepuestos.ENTREGADOS -> "Todavía no te entregaron repuestos"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Se piden desde el detalle de una incidencia que estés atendiendo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private val TamanoIcono = 20.dp

// ---------- Vistas previas ----------

private val ahoraDePrueba = Instant.parse("2026-11-07T15:00:00Z")

private fun deMuestra(
    id: String, pieza: String, estado: EstadoEntrega, horasAtras: Long, numero: String? = "1",
    despachada: Instant? = null, entregada: Instant? = null, llegada: Instant? = null
) = SolicitudConIncidencia(
    SolicitudRepuesto(
        id = id, incidenciaId = "i$id", pieza = pieza, motivo = "Motivo",
        fechaSolicitud = ahoraDePrueba.minus(Duration.ofHours(horasAtras)),
        confirmadoEn = ahoraDePrueba.minus(Duration.ofHours(horasAtras)),
        estadoEntrega = estado, solicitadoPorId = "t1", solicitadoPorNombre = "Luis Ramírez",
        numeroLogistica = numero, diasEnvio = 1, despachadaEn = despachada, entregadaEn = entregada, llegadaEstimada = llegada
    ),
    codigoIncidencia = "INC-0$id",
    codigoEquipo = "PC-LOG-009"
)

private val estadoDePrueba = EstadoRepuestosUiState(
    cargando = false,
    ultimaConsulta = ahoraDePrueba.minus(Duration.ofHours(7)),
    ahora = ahoraDePrueba,
    solicitudes = listOf(
        deMuestra("124", "Fuente de poder ATX 500 W", EstadoEntrega.POR_DESPACHAR, horasAtras = 5, numero = null),
        deMuestra("116", "Cable de video HDMI", EstadoEntrega.EN_CAMINO, horasAtras = 96,
            despachada = ahoraDePrueba.minus(Duration.ofHours(24)), llegada = ahoraDePrueba.plus(Duration.ofHours(20))),
        deMuestra("071", "Disco SSD 480 GB", EstadoEntrega.ENTREGADO, horasAtras = 24 * 80,
            despachada = ahoraDePrueba.minus(Duration.ofDays(79)), entregada = ahoraDePrueba.minus(Duration.ofDays(78)))
    )
)

@Composable
private fun Previa(estado: EstadoRepuestosUiState) = EstadoRepuestosScreen(estado, {}, {}, {}, {})

@Preview(name = "Estado de repuestos, claro", showBackground = true, heightDp = 900)
@Composable
private fun EstadoClaro() = SigtecTheme { Surface { Previa(estadoDePrueba) } }

@Preview(name = "Estado de repuestos, oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EstadoOscuro() = SigtecTheme(darkTheme = true) { Surface { Previa(estadoDePrueba.copy(sinRespuesta = true)) } }

@Preview(name = "Sin repuestos, claro", showBackground = true, heightDp = 600)
@Composable
private fun VacioClaro() = SigtecTheme { Surface { Previa(EstadoRepuestosUiState(cargando = false, ahora = ahoraDePrueba)) } }

@Preview(name = "Sin repuestos, oscuro", showBackground = true, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VacioOscuro() = SigtecTheme(darkTheme = true) {
    Surface { Previa(EstadoRepuestosUiState(cargando = false, ahora = ahoraDePrueba, filtro = FiltroRepuestos.PENDIENTES)) }
}
