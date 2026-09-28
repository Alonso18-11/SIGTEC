package com.softcorp.sigtec.feature.incidencias.presentation

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.AvisoConexion
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import java.time.Duration
import java.time.Instant

// Deja la última tarjeta por encima del botón «Registrar»
private val EspacioBotonFlotante = 88.dp

@Composable
fun ListaIncidenciasRoute(
    alAbrir: (String) -> Unit,
    alRegistrar: () -> Unit,
    vm: ListaIncidenciasViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    ListaIncidenciasScreen(
        estado = estado,
        alBuscar = vm::alBuscar,
        alFiltrarMarca = vm::alFiltrarMarca,
        alFiltrarTecnico = vm::alFiltrarTecnico,
        alFiltrarEquipo = vm::alFiltrarEquipo,
        alLimpiar = vm::limpiar,
        alAbrir = alAbrir,
        alRegistrar = alRegistrar,
        aviso = { AvisoConexion() }   // HU-05
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaIncidenciasScreen(
    estado: ListaIncidenciasUiState,
    alBuscar: (String) -> Unit,
    alFiltrarMarca: (MarcaSeguimiento?) -> Unit,
    alFiltrarTecnico: (String?) -> Unit,
    alFiltrarEquipo: (String?) -> Unit,
    alLimpiar: () -> Unit,
    alAbrir: (String) -> Unit,
    alRegistrar: () -> Unit,
    // HU-05: aviso de conexión. En las vistas previas queda vacío (AvisoConexion usa hiltViewModel)
    aviso: @Composable () -> Unit = {}
) {
    // La raíz ya aplica los márgenes de la barra de estado: aquí van en cero para no duplicarlos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Incidencias") }, windowInsets = WindowInsets(0)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alRegistrar,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Registrar") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Espaciado.m, end = Espaciado.m, bottom = EspacioBotonFlotante
            ),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            item { aviso() }
            item { Buscador(estado.filtros.texto, alBuscar) }
            item {
                Filtros(estado, alFiltrarMarca, alFiltrarTecnico, alFiltrarEquipo, alLimpiar)
            }
            when {
                estado.cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(Espaciado.xl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                estado.incidencias.isEmpty() -> item { SinResultados(hayDatos = estado.total > 0, alLimpiar) }
                else -> {
                    item { Contador(estado) }
                    items(estado.incidencias, key = { it.id }) { incidencia ->
                        TarjetaIncidencia(incidencia, estado.ahora, onClick = { alAbrir(incidencia.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Buscador(texto: String, alBuscar: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = alBuscar,
        placeholder = { Text("Buscar por código, equipo o problema") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { alBuscar("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        // Barra de búsqueda sin la línea inferior del campo de texto
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Filtros(
    estado: ListaIncidenciasUiState,
    alFiltrarMarca: (MarcaSeguimiento?) -> Unit,
    alFiltrarTecnico: (String?) -> Unit,
    alFiltrarEquipo: (String?) -> Unit,
    alLimpiar: () -> Unit
) {
    val f = estado.filtros
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        FiltroDesplegable("Estado", f.marca, MarcaSeguimiento.entries, { it.etiqueta() }, alFiltrarMarca)
        FiltroDesplegable("Técnico", f.tecnico, estado.opcionesTecnico, { it }, alFiltrarTecnico)
        FiltroDesplegable("Equipo", f.equipo, estado.opcionesEquipo, { it }, alFiltrarEquipo)
        if (f.activos) {
            AssistChip(
                onClick = alLimpiar,
                label = { Text("Limpiar filtros") },
                leadingIcon = { Icon(Icons.Outlined.FilterListOff, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun <T> FiltroDesplegable(
    titulo: String,
    seleccion: T?,
    opciones: List<T>,
    texto: (T) -> String,
    alElegir: (T?) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = seleccion != null,
            onClick = { abierto = true },
            label = { Text(seleccion?.let(texto) ?: titulo) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = { alElegir(null); abierto = false }
            )
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(texto(opcion)) },
                    onClick = { alElegir(opcion); abierto = false }
                )
            }
        }
    }
}

@Composable
private fun Contador(estado: ListaIncidenciasUiState) {
    val n = estado.incidencias.size
    val cantidad = when {
        estado.filtros.activos -> "$n de ${estado.total} incidencias"
        n == 1 -> "1 incidencia"
        else -> "$n incidencias"
    }
    Text(
        "$cantidad · de la más reciente a la más antigua",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TarjetaIncidencia(incidencia: Incidencia, ahora: Instant, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${incidencia.codigoParaMostrar()} · ${incidencia.codigoEquipo}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    fechaCorta(incidencia.fechaRegistro, ahora),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                Text(
                    incidencia.tecnicoAsignadoNombre ?: SIN_TECNICO,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SinResultados(hayDatos: Boolean, alLimpiar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Espaciado.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (hayDatos) "Ninguna incidencia coincide con la búsqueda."
            else "Aún no hay incidencias. Registra la primera con el botón «Registrar».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (hayDatos) TextButton(onClick = alLimpiar) { Text("Limpiar filtros") }
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Room ni ViewModel ----------

private val ahoraDePrueba = Instant.parse("2026-09-27T15:00:00Z")

private fun incidenciaDePrueba(
    numero: Int?, equipo: String, descripcion: String, marca: MarcaSeguimiento,
    tecnico: String?, horasAtras: Long, critica: Boolean = false
) = Incidencia(
    id = "p$numero$equipo", numero = numero, codigoEquipo = equipo, descripcion = descripcion,
    usuarioResponsable = "María Quispe", fechaRegistro = ahoraDePrueba.minus(Duration.ofHours(horasAtras)),
    registradoPorId = "t1", registradoPorNombre = "Luis Ramírez", marca = marca,
    tecnicoAsignadoNombre = tecnico, critica = critica
)

private val listaDePrueba = listOf(
    incidenciaDePrueba(null, "IMP-RRHH-002", "La impresora no aparece en la red", MarcaSeguimiento.SIN_ASIGNAR, null, 1),
    incidenciaDePrueba(126, "PC-VENT-031", "Pantalla azul al iniciar Windows", MarcaSeguimiento.ESPERANDO_REPUESTO, "Ana Torres", 2),
    incidenciaDePrueba(124, "PC-CONT-014", "No enciende después del corte de luz", MarcaSeguimiento.EN_ATENCION, "Luis Ramírez", 3),
    incidenciaDePrueba(121, "LAP-GER-005", "Sin conexión a la red Wi-Fi", MarcaSeguimiento.ASIGNADA, "Ana Torres", 150, critica = true),
    incidenciaDePrueba(119, "PC-ADM-022", "Teclas del teclado que no responden", MarcaSeguimiento.CERRADA, "Luis Ramírez", 170)
)

@Preview(name = "Lista · claro", showBackground = true, heightDp = 900)
@Composable
private fun ListaConDatos() = SigtecTheme {
    Surface {
        ListaIncidenciasScreen(
            ListaIncidenciasUiState(
                cargando = false, incidencias = listaDePrueba, total = listaDePrueba.size,
                opcionesTecnico = listOf(SIN_TECNICO, "Ana Torres", "Luis Ramírez"), ahora = ahoraDePrueba
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Filtrada · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListaFiltrada() = SigtecTheme(darkTheme = true) {
    Surface {
        ListaIncidenciasScreen(
            ListaIncidenciasUiState(
                cargando = false, incidencias = listaDePrueba.take(1), total = listaDePrueba.size,
                filtros = FiltrosIncidencias(marca = MarcaSeguimiento.SIN_ASIGNAR), ahora = ahoraDePrueba
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Sin resultados", showBackground = true)
@Composable
private fun ListaSinResultados() = SigtecTheme {
    Surface {
        ListaIncidenciasScreen(
            ListaIncidenciasUiState(
                cargando = false, total = 5, filtros = FiltrosIncidencias(texto = "monitor"), ahora = ahoraDePrueba
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
