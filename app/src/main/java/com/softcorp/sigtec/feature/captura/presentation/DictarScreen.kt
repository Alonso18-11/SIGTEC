package com.softcorp.sigtec.feature.captura.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPeligro
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado

/** HU-10 (RF-1019, RF-1021, RF-1022, RF-1017): la explicación del permiso de micrófono. */
val TextosPermisoMicrofono = TextosPermiso(
    rotuloPermitir = "Permitir micrófono",
    explicar = Explicacion(
        Icons.Outlined.Mic,
        "Permiso de micrófono",
        "SIGTEC usa el micrófono solo mientras dictas, para convertir tu voz en texto. No guarda el audio."
    ),
    negado = Explicacion(
        Icons.Outlined.MicOff,
        "Sin permiso de micrófono",
        "Sin el permiso de micrófono no se puede dictar."
    ),
    irAAjustes = Explicacion(
        Icons.Outlined.Settings,
        "Sin permiso de micrófono",
        "Android ya no vuelve a preguntar. Concede el permiso en los ajustes de SIGTEC, en Permisos → Micrófono."
    ),
    sinRecurso = Explicacion(
        Icons.Outlined.MicOff,
        "Sin reconocimiento de voz",
        "Este celular no tiene reconocimiento de voz."
    )
)

private const val TEXTO_SIN_MICROFONO = "Puedes escribir el texto en el formulario."

@Composable
fun DictarRoute(
    alVolver: () -> Unit,
    alUsarTexto: (CampoDictado, String) -> Unit,
    viewModel: DictadoViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val tieneMicrofono = remember {
        contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }
    // RF-1014: al dejar de estar a la vista se suelta el micrófono; también al rotar
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.alDejarDeVer() }
    // RF-1010: el texto se entrega una sola vez
    LaunchedEffect(estado.textoListo) {
        estado.textoListo?.let { texto ->
            alUsarTexto(estado.campo, texto)
            viewModel.alTextoEntregado()
        }
    }
    DictarScreen(
        estado = estado,
        alVolver = alVolver,   // RF-1012: al salir, ON_STOP suelta el micrófono
        alDetener = viewModel::alDetener,
        alSeguirDictando = viewModel::alSeguirDictando,
        alUsarTexto = viewModel::alUsarTexto,
        conPermiso = { contenido ->
            ConPermiso(
                permiso = Manifest.permission.RECORD_AUDIO,
                tieneRecurso = tieneMicrofono && !estado.sinServicio,   // RF-1017
                textos = TextosPermisoMicrofono,
                textoNegado = TEXTO_SIN_MICROFONO,
                modifier = Modifier.fillMaxSize()
            ) {
                LaunchedEffect(Unit) { viewModel.alPermisoConcedido() }   // RF-1004, RF-1020
                contenido()
            }
        }
    )
}

/** HU-10 (prototipo 15): el micrófono, el texto transcrito y las acciones. Solo dibuja. */
@Composable
fun DictarScreen(
    estado: DictadoUiState,
    alVolver: () -> Unit,
    alDetener: () -> Unit,
    alSeguirDictando: () -> Unit,
    alUsarTexto: () -> Unit,
    // Sin permiso se ve la explicación (RF-1019); las vistas previas no lo piden
    conPermiso: @Composable (contenido: @Composable () -> Unit) -> Unit = { contenido -> contenido() }
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior(tituloDictado(estado.campo), alVolver = alVolver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // BarraSuperior no tiene subtítulo: de dónde viene el dictado va debajo (RF-1003)
            if (estado.subtitulo.isNotBlank()) {
                Text(
                    estado.subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s)
                )
            }
            Box(Modifier.weight(1f)) {
                conPermiso { Contenido(estado, alDetener, alSeguirDictando, alUsarTexto) }
            }
        }
    }
}

@Composable
private fun Contenido(
    estado: DictadoUiState,
    alDetener: () -> Unit,
    alSeguirDictando: () -> Unit,
    alUsarTexto: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espaciado.m, vertical = Espaciado.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            Microfono(escuchando = estado.escuchando, alSeguirDictando = alSeguirDictando)
            Estado(estado)
            TextoTranscrito(estado)
            Text(
                "Habla con normalidad. Podrás corregir el texto antes de guardar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.m),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            if (estado.escuchando) {
                BotonPeligro(onClick = alDetener, modifier = Modifier.weight(1f)) {   // RF-1007
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Detener")
                }
            }
            BotonPrincipal(
                onClick = alUsarTexto,
                enabled = estado.puedeUsar,   // RF-1009
                modifier = Modifier.weight(1f)
            ) { Text("Usar este texto") }
        }
    }
}

/** El micrófono late mientras escucha; detenido, tocarlo vuelve a escuchar (RF-1007, RF-1008). */
@Composable
private fun Microfono(escuchando: Boolean, alSeguirDictando: () -> Unit) {
    val latido = rememberInfiniteTransition(label = "latido")
    val escala by latido.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DURACION_LATIDO_MS), RepeatMode.Reverse),
        label = "escala"
    )
    val anillos = if (escuchando) escala else 0.85f
    Box(Modifier.size(TamanoMicrofono), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(anillos)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        )
        Box(
            Modifier
                .size(TamanoMicrofono * 0.75f)
                .scale(anillos)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Box(
            Modifier
                .size(TamanoMicrofono * 0.5f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = !escuchando, role = Role.Button, onClick = alSeguirDictando)
                .semantics { contentDescription = if (escuchando) "Escuchando" else "Seguir dictando" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(TamanoIcono)
            )
        }
    }
}

@Composable
private fun Estado(estado: DictadoUiState) {
    val (texto, color) = when {
        estado.escuchando -> "Escuchando…" to MaterialTheme.colorScheme.primary
        estado.error != null -> mensajeDictado(estado.error) to MaterialTheme.colorScheme.error   // RF-1015 a RF-1018
        else -> "Toca el micrófono para seguir dictando" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(texto, style = MaterialTheme.typography.titleMedium, color = color, textAlign = TextAlign.Center)
}

/** RF-1005: lo ya reconocido en negro y lo que se está reconociendo en gris, hasta que se confirma. */
@Composable
private fun TextoTranscrito(estado: DictadoUiState) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Text(
                "Texto transcrito",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (estado.textoVisible.isBlank()) {
                Text(
                    "Lo que digas aparecerá aquí.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val pendiente = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    buildAnnotatedString {
                        append(estado.texto)
                        if (estado.parcial.isNotBlank()) {
                            if (estado.texto.isNotBlank()) append(" ")
                            withStyle(SpanStyle(color = pendiente)) { append(estado.parcial.trim()) }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private val TamanoMicrofono = 176.dp
private val TamanoIcono = 36.dp
private const val DURACION_LATIDO_MS = 700

// ---------- Vistas previas ----------

private val escuchando = DictadoUiState(
    campo = CampoDictado.DESCRIPCION,
    subtitulo = "Nueva incidencia · PC-CONT-014",
    escuchando = true,
    texto = "El equipo no enciende desde el corte de luz de esta mañana.",
    parcial = "No prende ninguna luz del"
)

private val detenido = escuchando.copy(
    escuchando = false,
    texto = "El equipo no enciende desde el corte de luz de esta mañana. No prende ninguna luz del case.",
    parcial = ""
)

private val conError = detenido.copy(campo = CampoDictado.DIAGNOSTICO, subtitulo = "Informe técnico · INC-0124",
    error = ErrorDictado.SIN_ENTENDER)

@Composable
private fun Previa(estado: DictadoUiState) = DictarScreen(estado, {}, {}, {}, {})

@Preview(name = "Escuchando · claro", showBackground = true, heightDp = 760)
@Composable
private fun EscuchandoClaro() = SigtecTheme { Previa(escuchando) }

@Preview(name = "Escuchando · oscuro", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EscuchandoOscuro() = SigtecTheme(darkTheme = true) { Previa(escuchando) }

@Preview(name = "Detenido con texto · claro", showBackground = true, heightDp = 760)
@Composable
private fun DetenidoClaro() = SigtecTheme { Previa(detenido) }

@Preview(name = "Detenido con texto · oscuro", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetenidoOscuro() = SigtecTheme(darkTheme = true) { Previa(detenido) }

@Preview(name = "Error · claro", showBackground = true, heightDp = 760)
@Composable
private fun ErrorClaro() = SigtecTheme { Previa(conError) }

@Preview(name = "Error · oscuro", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorOscuro() = SigtecTheme(darkTheme = true) { Previa(conError) }

@Composable
private fun PreviaPermiso(vista: VistaPermiso) = DictarScreen(
    estado = DictadoUiState(subtitulo = "Nueva incidencia"),
    alVolver = {}, alDetener = {}, alSeguirDictando = {}, alUsarTexto = {},
    conPermiso = {
        ExplicacionPermiso(vista, TextosPermisoMicrofono, TEXTO_SIN_MICROFONO, alPermitir = {}, alAbrirAjustes = {})
    }
)

@Preview(name = "Permiso de micrófono · claro", showBackground = true, heightDp = 760)
@Composable
private fun PermisoClaro() = SigtecTheme { PreviaPermiso(VistaPermiso.EXPLICAR) }

@Preview(name = "Permiso de micrófono · oscuro", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermisoOscuro() = SigtecTheme(darkTheme = true) { PreviaPermiso(VistaPermiso.EXPLICAR) }

@Preview(name = "Sin reconocimiento de voz · claro", showBackground = true, heightDp = 760)
@Composable
private fun SinServicioClaro() = SigtecTheme { PreviaPermiso(VistaPermiso.SIN_RECURSO) }

@Preview(name = "Sin reconocimiento de voz · oscuro", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SinServicioOscuro() = SigtecTheme(darkTheme = true) { PreviaPermiso(VistaPermiso.SIN_RECURSO) }
