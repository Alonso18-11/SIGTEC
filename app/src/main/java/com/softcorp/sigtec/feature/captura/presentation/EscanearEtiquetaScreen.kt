package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.feature.captura.domain.LecturaCodigo
import java.util.concurrent.Executors

// El marco imita la etiqueta de activo fijo: ancho, con proporción 2:1 (prototipo 13)
private const val ANCHO_MARCO = 0.85f
private const val PROPORCION_MARCO = 2f
private const val OPACIDAD_FUERA_DEL_MARCO = 0.55f
private val EsquinaMarco = 28.dp
private val GrosorMarco = 4.dp
private val RadioMarco = 8.dp
private val TamanoIconoCheck = 20.dp
private val TamanoIconoAviso = 18.dp

/**
 * HU-09 (RF-901): «Escanear etiqueta». Pide el permiso al entrar (RF-916) y, con él, lee el texto de la
 * cámara con ML Kit hasta reconocer un código.
 */
@Composable
fun EscanearEtiquetaRoute(
    alVolver: () -> Unit,
    alUsarCodigo: (String) -> Unit,
    vm: EscaneoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val lector = remember { LectorTextoEtiqueta(alLeerTexto = vm::alLeerTexto) }
    val hilo = remember { Executors.newSingleThreadExecutor() }
    // Solo la imagen más reciente: si ML Kit tarda, las de en medio se descartan
    val analisis = remember {
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    }
    val casos = remember { listOf<UseCase>(analisis) }
    // RF-906 y RF-908: con un código reconocido se deja de analizar; «Volver a escanear» lo reanuda
    val hayCodigo = estado.reconocido != null
    LaunchedEffect(hayCodigo) {
        if (hayCodigo) analisis.clearAnalyzer() else analisis.setAnalyzer(hilo, lector)
    }
    DisposableEffect(Unit) {
        onDispose {
            analisis.clearAnalyzer()
            lector.close()
            hilo.shutdown()
        }
    }
    EscanearEtiquetaScreen(
        estado = estado,
        alVolver = alVolver,
        alVolverAEscanear = vm::alVolverAEscanear,
        alUsarCodigo = alUsarCodigo,
        vistaCamara = { VistaCamara(casos, Modifier.fillMaxSize()) },
        conPermisoCamara = { contenido ->
            ConPermisoCamara(
                textoNegado = "También puedes escribir el código a mano en el formulario.", // RF-918
                modifier = Modifier.fillMaxSize(),
                contenido = contenido
            )
        }
    )
}

@Composable
fun EscanearEtiquetaScreen(
    estado: EscaneoUiState,
    alVolver: () -> Unit,
    alVolverAEscanear: () -> Unit,
    alUsarCodigo: (String) -> Unit,
    vistaCamara: @Composable () -> Unit,
    // Sin permiso se ve la explicación en lugar de la cámara y el marco (RF-916); las vistas previas no lo piden
    conPermisoCamara: @Composable (contenido: @Composable () -> Unit) -> Unit = { contenido -> contenido() }
) {
    // D-10: la barra clara del rediseño (#39) en vez de la barra oscura con «X» del prototipo
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Escanear etiqueta", alVolver = alVolver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                conPermisoCamara {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        vistaCamara()
                        MarcoEtiqueta(Modifier.fillMaxSize())
                        // La indicación va justo debajo del marco, sobre la imagen
                        val altoMarco = maxWidth * ANCHO_MARCO / PROPORCION_MARCO
                        Indicacion(
                            "Encuadra la etiqueta del equipo dentro del marco",
                            Modifier
                                .align(Alignment.Center)
                                .offset(y = altoMarco / 2 + Espaciado.l)
                        )
                    }
                }
            }
            // RF-906: nada se escribe en el formulario hasta que el usuario confirma
            estado.reconocido?.let { lectura ->
                PanelCodigoReconocido(lectura, alVolverAEscanear, alUsar = { alUsarCodigo(lectura.codigo) })
            }
        }
    }
}

/** Oscurece todo lo que queda fuera del marco y dibuja sus cuatro esquinas. */
@Composable
private fun MarcoEtiqueta(modifier: Modifier = Modifier) {
    val fuera = MaterialTheme.colorScheme.scrim.copy(alpha = OPACIDAD_FUERA_DEL_MARCO)
    val esquinas = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val ancho = size.width * ANCHO_MARCO
        val alto = ancho / PROPORCION_MARCO
        val origen = Offset((size.width - ancho) / 2, (size.height - alto) / 2)
        val radio = RadioMarco.toPx()
        val marco = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(RoundRect(origen.x, origen.y, origen.x + ancho, origen.y + alto, CornerRadius(radio)))
        }
        drawPath(marco, fuera)

        val largo = EsquinaMarco.toPx()
        val grosor = GrosorMarco.toPx()
        val fin = origen + Offset(ancho, alto)
        fun esquina(punto: Offset, dx: Float, dy: Float) {
            drawLine(esquinas, punto, punto + Offset(dx * largo, 0f), grosor, StrokeCap.Round)
            drawLine(esquinas, punto, punto + Offset(0f, dy * largo), grosor, StrokeCap.Round)
        }
        esquina(origen, 1f, 1f)
        esquina(Offset(fin.x, origen.y), -1f, 1f)
        esquina(Offset(origen.x, fin.y), 1f, -1f)
        esquina(fin, -1f, -1f)
    }
}

/** El código reconocido con sus dos salidas; si no está en el catálogo, lo avisa (RF-907). */
@Composable
private fun PanelCodigoReconocido(lectura: LecturaCodigo, alVolverAEscanear: () -> Unit, alUsar: () -> Unit) {
    val solucionado = SigtecTheme.estados.solucionado
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
                    Surface(color = solucionado.contenedor, contentColor = solucionado.contenido, shape = CircleShape) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.padding(Espaciado.s).size(TamanoIconoCheck))
                    }
                    Column {
                        Text(
                            "Código reconocido · sin conexión",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(lectura.codigo, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                if (!lectura.enCatalogo) AvisoFueraDeCatalogo()
                Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                    BotonSecundario(onClick = alVolverAEscanear, modifier = Modifier.weight(1f)) {
                        Text("Volver a escanear", maxLines = 1)
                    }
                    BotonPrincipal(onClick = alUsar, modifier = Modifier.weight(1f)) {
                        Text("Usar este código", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoFueraDeCatalogo() {
    val advertencia = SigtecTheme.estados.advertencia
    Surface(color = advertencia.contenedor, contentColor = advertencia.contenido, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espaciado.sm, vertical = Espaciado.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(TamanoIconoAviso))
            Text("No está en el catálogo de este celular", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Texto sobre la imagen de la cámara: en una pastilla para que se lea en claro y en oscuro. */
@Composable
private fun Indicacion(texto: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(horizontal = Espaciado.m)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.s)
        )
    }
}

/** Ocupa el lugar de la cámara en las vistas previas y hasta que se conecte (prototipo 14: «Vista de la cámara»). */
@Composable
internal fun VistaCamaraDeMuestra(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text("Vista de la cámara", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- Vistas previas: la pantalla se prueba sin cámara ni ViewModel ----------

@Preview(name = "Buscando · claro", showBackground = true, heightDp = 700)
@Composable
private fun BuscandoClaro() = SigtecTheme {
    Surface { Previa(EscaneoUiState()) }
}

@Preview(name = "Buscando · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun BuscandoOscuro() = SigtecTheme(darkTheme = true) {
    Surface { Previa(EscaneoUiState()) }
}

@Preview(name = "Reconocido · claro", showBackground = true, heightDp = 700)
@Composable
private fun ReconocidoClaro() = SigtecTheme { Surface { Previa(reconocido) } }

@Preview(name = "Reconocido · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun ReconocidoOscuro() = SigtecTheme(darkTheme = true) { Surface { Previa(reconocido) } }

@Preview(name = "Fuera del catálogo · claro", showBackground = true, heightDp = 700)
@Composable
private fun FueraDeCatalogoClaro() = SigtecTheme { Surface { Previa(fueraDeCatalogo) } }

@Preview(name = "Fuera del catálogo · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun FueraDeCatalogoOscuro() = SigtecTheme(darkTheme = true) { Surface { Previa(fueraDeCatalogo) } }

private val reconocido = EscaneoUiState(reconocido = LecturaCodigo("PC-CONT-014", enCatalogo = true))
private val fueraDeCatalogo = EscaneoUiState(reconocido = LecturaCodigo("LAP-GER-005", enCatalogo = false))

@Composable
private fun Previa(estado: EscaneoUiState) = EscanearEtiquetaScreen(
    estado = estado,
    alVolver = {},
    alVolverAEscanear = {},
    alUsarCodigo = {},
    vistaCamara = { VistaCamaraDeMuestra() }
)
