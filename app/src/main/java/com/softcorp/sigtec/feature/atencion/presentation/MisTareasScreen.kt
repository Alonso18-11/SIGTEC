package com.softcorp.sigtec.feature.atencion.presentation

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import com.softcorp.sigtec.feature.incidencias.presentation.diasPendiente
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasScreen(
    estado: MisTareasUiState,
    alAbrir: (String) -> Unit
) {
    // Destino del menú principal, igual que «Incidencias»: sin flecha de volver.
    // La raíz ya aplica los márgenes de la barra de estado; aquí van en cero para no duplicarlos.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Mis tareas") }, windowInsets = WindowInsets(0)) }
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

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                items(estado.incidencias, key = { it.id }) { incidencia ->
                    TarjetaMiTarea(incidencia, estado.ahora, onClick = { alAbrir(incidencia.id) })
                }
            }
        }
    }
}

// RF-803: en vez de una lista vacía, un mensaje de que no hay pendientes
@Composable
private fun SinPendientes() {
    Text(
        "No tienes incidencias pendientes.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = Espaciado.l)
    )
}

// RF-802: código visible (o «Por sincronizar»), equipo, descripción y la etiqueta de marca/crítica/días.
// Sin técnico (ya se sabe que son todas del mismo) ni filtros, a diferencia de ListaIncidenciasScreen.
@Composable
private fun TarjetaMiTarea(incidencia: Incidencia, ahora: Instant, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = AreaTactilMinima)
    ) {
        Column(
            Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
        ) {
            Text(
                "${incidencia.codigoParaMostrar()} · ${incidencia.codigoEquipo}",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                incidencia.descripcion,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaIncidencia(
                    marca = incidencia.marca,
                    critica = incidencia.critica,
                    diasPendiente = incidencia.diasPendiente(ahora)
                )
                Spacer(Modifier.weight(1f))
            }
        }
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
