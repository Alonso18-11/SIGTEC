package com.softcorp.sigtec.feature.diccionario.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraAccionInferior
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.CampoTexto
import com.softcorp.sigtec.core.ui.components.PestanasSegmentadas
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta

private const val LINEAS_TEXTO = 3
private val OPCIONES_TIPO: List<TipoFalla?> = TipoFalla.entries

@Composable
fun RegistrarFallaRoute(
    textosDictados: Map<CampoDictado, String?>,
    alConsumirDictado: (CampoDictado) -> Unit,
    alDictar: (CampoDictado) -> Unit,
    alVolver: () -> Unit,
    alGuardada: () -> Unit,
    vm: RegistrarFallaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    // HU-10 (RF-1011): cada texto dictado se aplica una sola vez a su campo y se borra
    LaunchedEffect(textosDictados) {
        textosDictados.forEach { (campo, texto) ->
            if (texto != null) {
                vm.alRecibirDictado(campo, texto)
                alConsumirDictado(campo)
            }
        }
    }
    LaunchedEffect(estado.guardada) {
        if (estado.guardada) {
            vm.alSalir()
            alGuardada()
        }
    }

    RegistrarFallaScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarTitulo = vm::alCambiarTitulo,
        alCambiarTipo = vm::alCambiarTipo,
        alCambiarComponente = vm::alCambiarComponente,
        alCambiarSintomas = vm::alCambiarSintomas,
        alCambiarSolucion = vm::alCambiarSolucion,
        alDictar = alDictar,
        alGuardar = vm::alGuardar
    )
}

/** HU-13 (RF-1310 a RF-1312): los cinco campos obligatorios de una falla. Solo dibuja. */
@Composable
fun RegistrarFallaScreen(
    estado: RegistrarFallaUiState,
    alVolver: () -> Unit,
    alCambiarTitulo: (String) -> Unit,
    alCambiarTipo: (TipoFalla?) -> Unit,
    alCambiarComponente: (String) -> Unit,
    alCambiarSintomas: (String) -> Unit,
    alCambiarSolucion: (String) -> Unit,
    alDictar: (CampoDictado) -> Unit,
    alGuardar: () -> Unit
) {
    val editable = !estado.guardando
    // BarraSuperior aplica el margen de la barra de estado; imePadding para que el teclado no tape «Guardar falla»
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Registrar falla", alVolver = alVolver) },
        bottomBar = { BarraGuardar(estado, alGuardar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            Text(
                estado.origen?.let { "Lleno con el informe de $it. Revisa los datos antes de guardar." }
                    ?: "Lo que resolviste queda para tus compañeros. Todos los campos son obligatorios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoTexto(
                valor = estado.titulo,
                alCambiar = alCambiarTitulo,
                etiqueta = "Título *",
                ayuda = "La falla en pocas palabras",
                habilitado = editable,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Text("Tipo *", style = MaterialTheme.typography.labelLarge)
                PestanasSegmentadas(
                    opciones = OPCIONES_TIPO,
                    seleccionada = estado.tipo,
                    texto = { it?.etiqueta().orEmpty() },
                    alElegir = { if (editable) alCambiarTipo(it) }
                )
            }
            CampoTexto(
                valor = estado.componente,
                alCambiar = alCambiarComponente,
                etiqueta = "Componente *",
                ayuda = "Por ejemplo: fuente de poder, impresora, Office",
                habilitado = editable,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )
            CampoTexto(
                valor = estado.sintomas,
                alCambiar = alCambiarSintomas,
                etiqueta = "Síntomas *",
                ayuda = "Qué se ve cuando ocurre",
                final = { Microfono("Dictar síntomas", editable) { alDictar(CampoDictado.SINTOMAS) } },
                unaLinea = false,
                lineasMinimas = LINEAS_TEXTO,
                habilitado = editable,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            CampoTexto(
                valor = estado.solucion,
                alCambiar = alCambiarSolucion,
                etiqueta = "Solución *",
                ayuda = "Qué se hizo para resolverla",
                final = { Microfono("Dictar solución", editable) { alDictar(CampoDictado.SOLUCION) } },
                unaLinea = false,
                lineasMinimas = LINEAS_TEXTO,
                habilitado = editable,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
    }
}

@Composable
private fun Microfono(descripcion: String, habilitado: Boolean, alDictar: () -> Unit) {
    // Pantallas v2: el micrófono es uno de los usos permitidos del acento (DESIGN.md 3)
    FilledIconButton(
        onClick = alDictar,
        enabled = habilitado,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = SigtecTheme.marca.acento,
            contentColor = SigtecTheme.marca.sobreAcento
        )
    ) {
        Icon(Icons.Outlined.Mic, contentDescription = descripcion)
    }
}

@Composable
private fun BarraGuardar(estado: RegistrarFallaUiState, alGuardar: () -> Unit) {
    BarraAccionInferior(
        aviso = estado.error?.let { error ->
            {
                Text(
                    error.mensaje(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) {
        BotonPrincipal(onClick = alGuardar, enabled = estado.puedeGuardar, modifier = Modifier.weight(1f)) {
            if (estado.guardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Espaciado.l),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Guardar falla")
            }
        }
    }
}

// ---------- Vistas previas ----------

@Composable
private fun Previa(estado: RegistrarFallaUiState) = Surface(color = MaterialTheme.colorScheme.background) {
    RegistrarFallaScreen(estado, {}, {}, {}, {}, {}, {}, {}, {})
}

private val llena = RegistrarFallaUiState(
    titulo = "La impresora no aparece en la red", tipo = TipoFalla.RED, componente = "Impresora",
    sintomas = "No se puede imprimir desde las PC del área.", solucion = "Asignar una IP fija y reiniciar la cola.",
    origen = "INC-0013 - IMP-RRHH-002"
)

@Preview(name = "Vacía, claro", showBackground = true, heightDp = 900)
@Composable
private fun RegistrarVacia() = SigtecTheme { Previa(RegistrarFallaUiState()) }

@Preview(name = "Desde una incidencia, oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrarDesdeIncidencia() = SigtecTheme(darkTheme = true) { Previa(llena) }
