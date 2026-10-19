package com.softcorp.sigtec.feature.atencion.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.FilaIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import com.softcorp.sigtec.feature.incidencias.presentation.diasPendiente
import com.softcorp.sigtec.feature.incidencias.presentation.fechaCorta
import java.time.Duration
import java.time.Instant

@Composable
fun MisTareasRoute(
    alAbrir: (String) -> Unit,
    vm: MisTareasViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    MisTareasScreen(estado = estado, alAbrir = alAbrir)
}

@Composable
fun MisTareasScreen(
    estado: MisTareasUiState,
    alAbrir: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Mis tareas") }
    ) { padding ->
        when {
            estado.cargando -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            estado.incidencias.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { SinPendientes() }

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { Resumen(estado.incidencias.size) }
                items(estado.incidencias, key = { it.id }) { incidencia ->
                    FilaIncidencia(
                        codigo = incidencia.codigoParaMostrar(),
                        equipo = incidencia.codigoEquipo,
                        descripcion = incidencia.descripcion,
                        marca = incidencia.marca,
                        critica = incidencia.critica,
                        diasPendiente = incidencia.diasPendiente(estado.ahora),
                        onClick = { alAbrir(incidencia.id) },
                        fecha = fechaCorta(incidencia.fechaRegistro, estado.ahora)
                    )
                }
            }
        }
    }
}

@Composable
private fun Resumen(cantidad: Int) {
    Column {
        Text(
            "${if (cantidad == 1) "1 pendiente" else "$cantidad pendientes"} · de la más antigua a la más reciente",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Espaciado.m, end = Espaciado.m, top = Espaciado.m, bottom = Espaciado.s)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SinPendientes() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Espaciado.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        Icon(
            Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = SigtecTheme.estados.solucionado.contenido,
            modifier = Modifier.size(40.dp)
        )
        Text("Todo al día", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "No tienes incidencias pendientes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val ahoraDePrueba = Instant.parse("2026-09-27T15:00:00Z")

private fun incidenciaDePrueba(
    numero: Int?, equipo: String, descripcion: String, marca: MarcaSeguimiento,
    horasAtras: Long, critica: Boolean = false
) = Incidencia(
    id = "p$numero$equipo", numero = numero, codigoEquipo = equipo, descripcion = descripcion,
    usuarioResponsable = "María Quispe", fechaRegistro = ahoraDePrueba.minus(Duration.ofHours(horasAtras)),
    registradoPorId = "t1", registradoPorNombre = "Luis Ramírez", marca = marca,
    tecnicoAsignadoNombre = "Luis Ramírez", critica = critica
)

private val bandejaDePrueba = listOf(
    incidenciaDePrueba(121, "LAP-GER-005", "Sin conexión a la red Wi-Fi", MarcaSeguimiento.ASIGNADA, 150, critica = true),
    incidenciaDePrueba(124, "PC-CONT-014", "No enciende después del corte de luz", MarcaSeguimiento.EN_ATENCION, 3),
    incidenciaDePrueba(126, "PC-VENT-031", "Pantalla azul al iniciar Windows", MarcaSeguimiento.ESPERANDO_REPUESTO, 2)
)

@Preview(name = "Bandeja con tareas · claro", showBackground = true, heightDp = 700)
@Composable
private fun MisTareasConDatos() = SigtecTheme {
    Surface {
        MisTareasScreen(
            estado = MisTareasUiState(cargando = false, incidencias = bandejaDePrueba, ahora = ahoraDePrueba),
            alAbrir = {}
        )
    }
}

@Preview(name = "Bandeja con tareas · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun MisTareasConDatosOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        MisTareasScreen(
            estado = MisTareasUiState(cargando = false, incidencias = bandejaDePrueba, ahora = ahoraDePrueba),
            alAbrir = {}
        )
    }
}

@Preview(name = "Bandeja vacía · claro", showBackground = true)
@Composable
private fun MisTareasVacia() = SigtecTheme {
    Surface {
        MisTareasScreen(estado = MisTareasUiState(cargando = false), alAbrir = {})
    }
}

@Preview(name = "Bandeja vacía · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MisTareasVaciaOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        MisTareasScreen(estado = MisTareasUiState(cargando = false), alAbrir = {})
    }
}
