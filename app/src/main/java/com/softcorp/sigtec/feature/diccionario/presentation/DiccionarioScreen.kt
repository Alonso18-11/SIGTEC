package com.softcorp.sigtec.feature.diccionario.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.FormaCompleta
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.FabExtendido
import com.softcorp.sigtec.core.ui.components.FiltrosChips
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta
import java.time.Instant

// Deja la última tarjeta por encima del botón «Registrar falla»
private val EspacioFab = 88.dp

// RF-1305: «Todas» es null
private val OPCIONES_TIPO: List<TipoFalla?> = listOf(null) + TipoFalla.entries

@Composable
fun DiccionarioRoute(
    aviso: String?,
    alRegistrar: () -> Unit,
    vm: DiccionarioViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    // RF-1313: «Falla registrada en el diccionario» al volver del formulario
    LaunchedEffect(aviso) { aviso?.let { avisos.showSnackbar(it) } }
    DiccionarioScreen(
        estado = estado,
        avisos = avisos,
        alBuscar = vm::alBuscar,
        alFiltrarTipo = vm::alFiltrarTipo,
        alTocarFalla = vm::alTocarFalla,
        alRegistrar = alRegistrar
    )
}

/** HU-13 (prototipo 14): título grande, buscador, filtros por tipo y las fallas en tarjetas. Solo dibuja. */
@Composable
fun DiccionarioScreen(
    estado: DiccionarioUiState,
    alBuscar: (String) -> Unit,
    alFiltrarTipo: (TipoFalla?) -> Unit,
    alTocarFalla: (String) -> Unit,
    alRegistrar: () -> Unit,
    avisos: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(avisos) },
        topBar = { Titulo(estado) },
        // RF-1309: solo quien puede registrar ve el botón (en acento, prototipo 14)
        floatingActionButton = { if (estado.puedeRegistrar) FabExtendido("Registrar falla", onClick = alRegistrar) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                estado.sinPermiso -> Mensaje("No tienes acceso al diccionario de fallas.")
                else -> Lista(estado, alBuscar, alFiltrarTipo, alTocarFalla)
            }
        }
    }
}

@Composable
private fun Titulo(estado: DiccionarioUiState) {
    Column(
        Modifier
            .statusBarsPadding()
            .padding(start = Espaciado.m, end = Espaciado.m, top = Espaciado.l, bottom = Espaciado.xs)
    ) {
        Text(
            "Diccionario de fallas",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )
        if (!estado.cargando && !estado.sinPermiso) {
            Text(
                cantidadFallas(estado.total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Lista(
    estado: DiccionarioUiState,
    alBuscar: (String) -> Unit,
    alFiltrarTipo: (TipoFalla?) -> Unit,
    alTocarFalla: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = EspacioFab),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        item {
            Box(Modifier.padding(start = Espaciado.m, end = Espaciado.m, top = Espaciado.s)) {
                Buscador(estado.texto, alBuscar)
            }
        }
        item {
            FiltrosChips(
                opciones = OPCIONES_TIPO,
                seleccionada = estado.tipo,
                texto = { it?.etiqueta() ?: "Todas" },
                alElegir = alFiltrarTipo
            )
        }
        if (estado.fallas.isEmpty()) {
            item { Vacio(estado) }
        }
        items(estado.fallas, key = { it.id }) { falla ->
            TarjetaFalla(
                falla = falla,
                desplegada = estado.desplegadaId == falla.id,
                alTocar = { alTocarFalla(falla.id) },
                modifier = Modifier.padding(horizontal = Espaciado.m)
            )
        }
    }
}

/** RF-1307: sin fallas, o sin resultados para la búsqueda. */
@Composable
private fun Vacio(estado: DiccionarioUiState) {
    val texto = when {
        estado.total > 0 && estado.hayFiltro -> "No hay fallas que coincidan con la búsqueda."
        estado.puedeRegistrar -> "Aún no hay fallas en el diccionario.\nRegistra la primera con el botón «Registrar falla»."
        else -> "Aún no hay fallas en el diccionario."
    }
    Text(
        texto,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.m, vertical = Espaciado.xl)
    )
}

@Composable
private fun Mensaje(texto: String) {
    Box(Modifier.fillMaxSize().padding(Espaciado.m), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** El mismo buscador en píldora blanca que la lista de incidencias (prototipos 03 y 14). */
@Composable
private fun Buscador(texto: String, alBuscar: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = alBuscar,
        placeholder = { Text("Síntoma, componente o palabra clave", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { alBuscar("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = FormaCompleta,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(Elevacion.tarjeta, FormaCompleta)
    )
}

// ---------- Vistas previas ----------

private val fecha = Instant.parse("2026-11-01T10:00:00Z")

private fun fallaDePrueba(id: String, titulo: String, tipo: TipoFalla, componente: String, solucion: String, autor: String) =
    FallaDiccionario(id, titulo, "No enciende después de un corte de luz", componente, tipo, solucion, "u-$id", autor, fecha)

private val fallasDePrueba = listOf(
    fallaDePrueba("1", "Fuente de poder dañada por variación de voltaje", TipoFalla.HARDWARE, "Fuente de poder",
        "Reemplazar la fuente y revisar el estabilizador.", "Luis Ramírez"),
    fallaDePrueba("2", "La impresora no aparece en la red", TipoFalla.RED, "Impresora",
        "Asignar una IP fija y reiniciar la cola de impresión.", "Ana Torres"),
    fallaDePrueba("3", "Pantalla azul al iniciar Windows", TipoFalla.SOFTWARE, "Sistema operativo",
        "Iniciar en modo seguro y revertir el último controlador instalado.", "Jorge Salas")
)

private val estadoDePrueba = DiccionarioUiState(
    cargando = false, total = 3, fallas = fallasDePrueba, puedeRegistrar = true, desplegadaId = "2"
)

@Composable
private fun Previa(estado: DiccionarioUiState) = Surface(color = MaterialTheme.colorScheme.background) {
    DiccionarioScreen(estado, alBuscar = {}, alFiltrarTipo = {}, alTocarFalla = {}, alRegistrar = {})
}

@Preview(name = "Diccionario, claro", showBackground = true, heightDp = 900)
@Composable
private fun DiccionarioClaro() = SigtecTheme { Previa(estadoDePrueba) }

@Preview(name = "Diccionario, oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiccionarioOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoDePrueba) }

@Preview(name = "Vacío, personal de sistemas, claro", showBackground = true)
@Composable
private fun DiccionarioVacio() = SigtecTheme { Previa(DiccionarioUiState(cargando = false)) }

@Preview(name = "Sin resultados, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiccionarioSinResultados() = SigtecTheme(darkTheme = true) {
    Previa(DiccionarioUiState(cargando = false, total = 3, texto = "mouse", puedeRegistrar = true))
}
