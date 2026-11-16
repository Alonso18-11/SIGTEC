package com.softcorp.sigtec.feature.indicadores.presentation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.CabeceraHero
import com.softcorp.sigtec.core.ui.components.CampoTexto
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado
import com.softcorp.sigtec.core.ui.components.FondoBarraEstado
import com.softcorp.sigtec.core.ui.components.IconosBarraEstadoClaros
import com.softcorp.sigtec.core.ui.components.TarjetaMetrica
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.indicadores.domain.HistorialEquipo
import java.time.Instant

private const val TITULO = "Historial del equipo"

@Composable
fun HistorialEquipoRoute(
    codigoEscaneado: String?,
    alConsumirCodigo: () -> Unit,
    alVolver: () -> Unit,
    alEscanear: () -> Unit,
    alAbrirIncidencia: (String) -> Unit,
    vm: HistorialEquipoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    // RF-1605: el código que deja «Escanear etiqueta» se busca solo, y se consume para no repetirlo al volver
    LaunchedEffect(codigoEscaneado) {
        codigoEscaneado?.let {
            vm.alUsarCodigoEscaneado(it)
            alConsumirCodigo()
        }
    }
    // Un historial buscado vuelve al formulario; uno que llegó con código sale de la pantalla
    BackHandler(enabled = estado.puedeVolverABuscar) { vm.alVolverABuscar() }
    HistorialEquipoScreen(
        estado = estado,
        alVolver = { if (estado.puedeVolverABuscar) vm.alVolverABuscar() else alVolver() },
        alCambiarTexto = vm::alCambiarTexto,
        alBuscar = vm::alBuscar,
        alEscanear = alEscanear,
        alAbrirIncidencia = alAbrirIncidencia
    )
}

/** HU-16 (prototipo 11): la búsqueda sin código, o la cabecera del equipo con sus atenciones. Solo dibuja. */
@Composable
fun HistorialEquipoScreen(
    estado: HistorialEquipoUiState,
    alVolver: () -> Unit,
    alCambiarTexto: (String) -> Unit,
    alBuscar: () -> Unit,
    alEscanear: () -> Unit,
    alAbrirIncidencia: (String) -> Unit
) {
    val historial = estado.historial
    when {
        estado.codigo == null -> Busqueda(estado, alVolver, alCambiarTexto, alBuscar, alEscanear)
        estado.error != null -> Mensaje(estado.error, alVolver)
        historial == null -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        else -> Historial(historial, estado.ahora, alVolver, alAbrirIncidencia)
    }
}

/** RF-1603, RF-1604: el código escrito o escaneado. */
@Composable
private fun Busqueda(
    estado: HistorialEquipoUiState,
    alVolver: () -> Unit,
    alCambiarTexto: (String) -> Unit,
    alBuscar: () -> Unit,
    alEscanear: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior(TITULO, alVolver = alVolver) }
    ) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            Text(
                "Escribe el código de la etiqueta o escanéala para ver las atenciones del equipo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoTexto(
                valor = estado.texto,
                alCambiar = alCambiarTexto,
                etiqueta = "Código del equipo",
                final = {
                    IconButton(onClick = alEscanear) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Escanear etiqueta")
                    }
                },
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                acciones = KeyboardActions(onSearch = { alBuscar() })
            )
            BotonPrincipal(onClick = alBuscar, enabled = estado.puedeBuscar, modifier = Modifier.fillMaxWidth()) {
                Text("Ver historial")
            }
        }
    }
}

/** RF-1602: sin permiso (o sin sesión), el mensaje del error en lugar del historial. */
@Composable
private fun Mensaje(error: ErrorDominio, alVolver: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior(TITULO, alVolver = alVolver) }
    ) { relleno ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            Text(
                error.mensaje(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Espaciado.m)
            )
        }
    }
}

@Composable
private fun Historial(
    historial: HistorialEquipo,
    ahora: Instant,
    alVolver: () -> Unit,
    alAbrirIncidencia: (String) -> Unit
) {
    IconosBarraEstadoClaros()   // la cabecera oscura queda detrás de la barra de estado
    val desplazamiento = rememberScrollState()
    val desplazado by remember { derivedStateOf { desplazamiento.value > 0 } }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(desplazamiento)
        ) {
            Cabecera(historial, alVolver)
            Column(
                Modifier.padding(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.m)
            ) {
                if (historial.critico) AvisoReemplazo()
                Text(
                    "Atenciones recibidas",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                if (historial.atenciones.isEmpty()) {
                    // RF-1621
                    Text(
                        "Este equipo no tiene atenciones registradas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        historial.atenciones.forEachIndexed { i, a ->
                            FilaAtencion(
                                atencion = a,
                                ahora = ahora,
                                ultima = i == historial.atenciones.lastIndex,
                                alAbrir = { alAbrirIncidencia(a.incidenciaId) }   // RF-1620
                            )
                        }
                    }
                }
            }
        }
        FondoBarraEstado(visible = desplazado)
    }
}

/** RF-1607 a RF-1613: volver, «Equipo crítico», el código, el catálogo y las tres métricas de 3 meses. */
@Composable
private fun Cabecera(historial: HistorialEquipo, alVolver: () -> Unit) {
    val marca = SigtecTheme.marca
    CabeceraHero {
        Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = marca.sobreHero)
            }
            Spacer(Modifier.weight(1f))
            if (historial.critico) {
                EtiquetaEstado("Equipo crítico", SigtecTheme.estados.critico, icono = Icons.Outlined.ErrorOutline)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Text("Código del equipo", style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue)
            Text(
                historial.codigo,
                style = MaterialTheme.typography.headlineLarge,
                color = marca.sobreHero,
                modifier = Modifier.semantics { heading() }
            )
            // RF-1607, RF-1608
            val lineas = historial.equipo?.let(::lineasEquipo) ?: listOf("Equipo fuera del catálogo")
            lineas.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue) }
        }
        // RF-1609: las tres a la misma altura aunque una etiqueta ocupe más líneas
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            val alto = Modifier
                .weight(1f)
                .fillMaxHeight()
            val promedio = metricaPromedio(historial.horasPromedioTresMeses)
            TarjetaMetrica("${historial.atencionesTresMeses}", "Atenciones en 3 meses", alto, enCabecera = true)
            TarjetaMetrica("${historial.piezasTresMeses}", "Piezas cambiadas en 3 meses", alto, enCabecera = true)
            TarjetaMetrica(promedio.valor, promedio.etiqueta, alto, enCabecera = true)
        }
    }
}

/** RF-1613: el aviso de reemplazo, en el ámbar claro de los avisos (`advertencia`). */
@Composable
private fun AvisoReemplazo() {
    val color = SigtecTheme.estados.advertencia
    Surface(color = color.contenedor, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Espaciado.m),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = color.contenido, modifier = Modifier.size(24.dp))
            Text(
                "Este equipo tuvo fallas reiteradas en los últimos tres meses. Estos datos sirven para evaluar su reemplazo.",
                style = MaterialTheme.typography.bodyMedium,
                color = color.contenido,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------- Vistas previas ----------

private val equipoDePrueba = Equipo(
    "PC-CONT-014", "Computadora de escritorio", "HP", "ProDesk 400", "Contabilidad", "María Quispe Huamán"
)

private val historialDePrueba = HistorialEquipo(
    codigo = "PC-CONT-014",
    equipo = equipoDePrueba,
    atencionesTresMeses = 4,
    piezasTresMeses = 3,
    horasPromedioTresMeses = 50.4,
    critico = true,
    atenciones = atencionesDePrueba
)

private val estadoDePrueba = HistorialEquipoUiState(
    codigo = "PC-CONT-014",
    historial = historialDePrueba,
    ahora = Instant.parse("2026-09-28T15:00:00Z")
)

@Composable
private fun Previa(estado: HistorialEquipoUiState) = Surface(color = MaterialTheme.colorScheme.background) {
    HistorialEquipoScreen(estado, alVolver = {}, alCambiarTexto = {}, alBuscar = {}, alEscanear = {}, alAbrirIncidencia = {})
}

@Preview(name = "Historial crítico, claro", showBackground = true, heightDp = 1100)
@Composable
private fun HistorialClaro() = SigtecTheme { Previa(estadoDePrueba) }

@Preview(
    name = "Historial crítico, oscuro", showBackground = true, heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HistorialOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoDePrueba) }

@Preview(name = "Fuera del catálogo y sin atenciones, claro", showBackground = true, heightDp = 700)
@Composable
private fun HistorialVacio() = SigtecTheme {
    Previa(
        estadoDePrueba.copy(
            codigo = "PC-NUEVA-001",
            historial = HistorialEquipo("PC-NUEVA-001", null, 0, 0, null, false, emptyList())
        )
    )
}

@Preview(
    name = "Fuera del catálogo y sin atenciones, oscuro", showBackground = true, heightDp = 700,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HistorialVacioOscuro() = SigtecTheme(darkTheme = true) {
    Previa(
        estadoDePrueba.copy(
            codigo = "PC-NUEVA-001",
            historial = HistorialEquipo("PC-NUEVA-001", null, 0, 0, null, false, emptyList())
        )
    )
}

@Preview(name = "Búsqueda, claro", showBackground = true)
@Composable
private fun BusquedaClaro() = SigtecTheme { Previa(HistorialEquipoUiState(modoBusqueda = true, texto = "pc-cont")) }

@Preview(name = "Búsqueda, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BusquedaOscuro() = SigtecTheme(darkTheme = true) { Previa(HistorialEquipoUiState(modoBusqueda = true)) }
