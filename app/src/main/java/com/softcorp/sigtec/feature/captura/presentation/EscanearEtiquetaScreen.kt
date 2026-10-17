package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// El marco imita la etiqueta de activo fijo: ancho, con proporción 2:1 (prototipo 13)
private const val ANCHO_MARCO = 0.85f
private const val PROPORCION_MARCO = 2f
private const val OPACIDAD_FUERA_DEL_MARCO = 0.55f
private val EsquinaMarco = 28.dp
private val GrosorMarco = 4.dp
private val RadioMarco = 8.dp

/**
 * HU-09 (RF-901): «Escanear etiqueta». La cámara llega en PR-16; mientras tanto la pantalla muestra
 * el recuadro de muestra, igual que sus vistas previas.
 */
@Composable
fun EscanearEtiquetaRoute(
    alVolver: () -> Unit,
    vm: EscaneoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    EscanearEtiquetaScreen(
        estado = estado,
        alVolver = alVolver,
        vistaCamara = { VistaCamaraDeMuestra() }
    )
}

@Composable
fun EscanearEtiquetaScreen(
    estado: EscaneoUiState,
    alVolver: () -> Unit,
    vistaCamara: @Composable () -> Unit
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
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
    Surface { EscanearEtiquetaScreen(EscaneoUiState(), alVolver = {}, vistaCamara = { VistaCamaraDeMuestra() }) }
}

@Preview(name = "Buscando · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun BuscandoOscuro() = SigtecTheme(darkTheme = true) {
    Surface { EscanearEtiquetaScreen(EscaneoUiState(), alVolver = {}, vistaCamara = { VistaCamaraDeMuestra() }) }
}
