package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPeligro
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.feature.incidencias.presentation.fechaCompleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * HU-09 (RF-934, RF-935, RF-937): la foto a pantalla completa, con su tipo y su fecha. Se cierra con «Volver» o con
 * el botón atrás. «Eliminar foto» solo se ve con [puedeEliminar]; la regla se vuelve a verificar en `EliminarEvidencia`.
 */
@Composable
fun FotoAmpliada(
    foto: Evidencia,
    alVolver: () -> Unit,
    puedeEliminar: Boolean = false,
    eliminando: Boolean = false,
    error: String? = null,
    alEliminar: () -> Unit = {}
) {
    Dialog(onDismissRequest = alVolver, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ContenidoFotoAmpliada(foto.tipo, foto.fechaCaptura, alVolver, puedeEliminar, eliminando, error, alEliminar) { modifier ->
            ImagenAmpliada(foto.rutaArchivo, rotuloDeEvidencia(foto.tipo), modifier)
        }
    }
}

@Composable
private fun ContenidoFotoAmpliada(
    tipo: TipoEvidencia,
    fecha: Instant,
    alVolver: () -> Unit,
    puedeEliminar: Boolean,
    eliminando: Boolean,
    error: String?,
    alEliminar: () -> Unit,
    imagen: @Composable (Modifier) -> Unit
) {
    var confirmando by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            BarraSuperior(rotuloDeEvidencia(tipo), alVolver = alVolver)
            imagen(Modifier.weight(1f).fillMaxWidth())
            Column(
                modifier = Modifier.fillMaxWidth().padding(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                Text(
                    "Tomada el ${fechaCompleta(fecha)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                if (puedeEliminar) {
                    BotonPeligro(
                        onClick = { confirmando = true },
                        enabled = !eliminando,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Eliminar foto") }
                }
            }
        }
    }
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Eliminar esta foto?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = {
                BotonTexto(
                    onClick = { confirmando = false; alEliminar() },
                    color = MaterialTheme.colorScheme.error
                ) { Text("Eliminar") }
            },
            dismissButton = { BotonTexto(onClick = { confirmando = false }) { Text("Cancelar") } }
        )
    }
}

/** La foto completa, sin recortar; se decodifica fuera del hilo principal y, si no se puede leer, lo avisa (RF-935). */
@Composable
private fun ImagenAmpliada(ruta: String, descripcion: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val ladoPx = with(LocalDensity.current) { maxOf(maxWidth, maxHeight).roundToPx() }
        val carga by produceState<CargaFoto>(CargaFoto.Cargando, ruta, ladoPx) {
            value = withContext(Dispatchers.IO) { decodificarReducida(ruta, ladoPx) }
                ?.let { CargaFoto.Lista(it.asImageBitmap()) }
                ?: CargaFoto.NoDisponible
        }
        when (val actual = carga) {
            CargaFoto.Cargando -> Unit
            CargaFoto.NoDisponible -> FotoNoDisponible()
            is CargaFoto.Lista -> Image(
                actual.imagen,
                contentDescription = descripcion,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FotoNoDisponible(modifier: Modifier = Modifier) {
    Text(
        "Foto no disponible",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

// ---------- Vistas previas ----------

private val fechaDePrueba = Instant.parse("2026-10-18T15:00:00Z")

@Composable
private fun Previa(puedeEliminar: Boolean, error: String? = null) = ContenidoFotoAmpliada(
    TipoEvidencia.FALLA, fechaDePrueba, {}, puedeEliminar, eliminando = false, error, {}
) { modifier ->
    Box(modifier, contentAlignment = Alignment.Center) { FotoNoDisponible() }
}

@Preview(name = "Técnico asignado, claro", showBackground = true, heightDp = 700)
@Composable
private fun AmpliadaConEliminarClaro() = SigtecTheme { Previa(puedeEliminar = true) }

@Preview(name = "Técnico asignado, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun AmpliadaConEliminarOscuro() = SigtecTheme(darkTheme = true) { Previa(puedeEliminar = true) }

@Preview(name = "Otro perfil, claro", showBackground = true, heightDp = 700)
@Composable
private fun AmpliadaSinEliminarClaro() = SigtecTheme { Previa(puedeEliminar = false) }

@Preview(name = "Con error, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 700)
@Composable
private fun AmpliadaConErrorOscuro() = SigtecTheme(darkTheme = true) {
    Previa(puedeEliminar = true, error = "Ya no puedes eliminar fotos de esta incidencia.")
}
