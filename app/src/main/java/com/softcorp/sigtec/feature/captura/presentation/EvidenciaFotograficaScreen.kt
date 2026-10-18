package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import java.time.Instant

/**
 * HU-09 (RF-924, prototipo 14): «Evidencia fotográfica». La cámara llega en PR-36; mientras tanto la pantalla
 * muestra el recuadro de muestra, igual que sus vistas previas.
 */
@Composable
fun EvidenciaFotograficaRoute(
    alVolver: () -> Unit,
    vm: CapturaEvidenciaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    EvidenciaFotograficaScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarTipo = vm::alCambiarTipo,
        vistaCamara = { VistaCamaraDeMuestra() }
    )
}

@Composable
fun EvidenciaFotograficaScreen(
    estado: CapturaEvidenciaUiState,
    alVolver: () -> Unit,
    alCambiarTipo: (TipoEvidencia) -> Unit,
    vistaCamara: @Composable () -> Unit
) {
    // D-10: la barra clara del rediseño (#39) en vez de la barra oscura con «X» del prototipo
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Evidencia fotográfica", alVolver = alVolver) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            // BarraSuperior no tiene subtítulo: el código de la incidencia y el del equipo van debajo
            estado.incidencia?.let { incidencia ->
                Text(
                    "${incidencia.codigoVisible ?: POR_SINCRONIZAR} · ${incidencia.codigoEquipo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SelectorTipo(estado.tipo, alCambiarTipo)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                vistaCamara()
                Indicacion(
                    "Se guardará como: ${nombreDelTipo(estado.tipo)}",
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Espaciado.m)
                )
            }
        }
    }
}

/** RF-924: «Falla» o «Pieza cambiada», con las esquinas del tema en vez de la píldora de Material 3. */
@Composable
private fun SelectorTipo(tipo: TipoEvidencia, alCambiarTipo: (TipoEvidencia) -> Unit) {
    val opciones = listOf(TipoEvidencia.FALLA to "Falla", TipoEvidencia.PIEZA_CAMBIADA to "Pieza cambiada")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, (opcion, texto) ->
            SegmentedButton(
                selected = tipo == opcion,
                onClick = { alCambiarTipo(opcion) },
                shape = SegmentedButtonDefaults.itemShape(indice, opciones.size, MaterialTheme.shapes.small)
            ) { Text(texto) }
        }
    }
}

internal fun nombreDelTipo(tipo: TipoEvidencia): String = when (tipo) {
    TipoEvidencia.FALLA -> "foto de la falla"
    TipoEvidencia.PIEZA_CAMBIADA -> "foto de la pieza cambiada"
}

// ---------- Vistas previas: la pantalla se prueba sin Room, ViewModel ni cámara ----------

private val incidenciaDePrueba = Incidencia(
    id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
    usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
    registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
    marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
)

private val listaParaTomar = CapturaEvidenciaUiState(cargando = false, incidencia = incidenciaDePrueba)

@Preview(name = "Evidencia · claro", showBackground = true, heightDp = 760)
@Composable
private fun EvidenciaClaro() = SigtecTheme { Surface { Previa(listaParaTomar) } }

@Preview(name = "Evidencia · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 760)
@Composable
private fun EvidenciaOscuro() = SigtecTheme(darkTheme = true) {
    Surface { Previa(listaParaTomar.copy(tipo = TipoEvidencia.PIEZA_CAMBIADA)) }
}

@Composable
private fun Previa(estado: CapturaEvidenciaUiState) = EvidenciaFotograficaScreen(
    estado = estado,
    alVolver = {},
    alCambiarTipo = {},
    vistaCamara = { VistaCamaraDeMuestra() }
)
