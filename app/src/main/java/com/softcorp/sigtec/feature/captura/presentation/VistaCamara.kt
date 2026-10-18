package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import androidx.compose.ui.tooling.preview.Preview as VistaPrevia

private const val TAG = "VistaCamara"

/**
 * La imagen de la cámara con los [casos] que pida cada pantalla (`ImageAnalysis` al escanear, `ImageCapture`
 * al tomar fotos). La cámara queda atada al ciclo de vida de la pantalla: se suelta cuando deja de estar a la
 * vista y al salir de ella (RF-915). [casos] debe recordarse (`remember`), porque cambiarlo vuelve a abrir la cámara.
 */
@Composable
fun VistaCamara(casos: List<UseCase>, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current
    val vista = remember { PreviewView(contexto).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val previa = remember { Preview.Builder().build() }
    var fallo by remember { mutableStateOf(false) }
    var intento by remember { mutableIntStateOf(0) }

    LaunchedEffect(casos, cicloDeVida, intento) {
        fallo = false
        val todos = arrayOf(previa, *casos.toTypedArray())
        var proveedor: ProcessCameraProvider? = null
        var dejarDeObservar: () -> Unit = {}
        try {
            proveedor = ProcessCameraProvider.awaitInstance(contexto)
            previa.setSurfaceProvider(vista.surfaceProvider)
            val camara = proveedor.bindToLifecycle(cicloDeVida, camaraDisponible(proveedor), *todos)
            // RF-921: la cámara se abre después; si otra app la usa, el error llega por su estado.
            // Mientras abre, CameraX reintenta solo los errores pasajeros, así que esos no se avisan.
            val estado = camara.cameraInfo.cameraState
            val observador = Observer<CameraState> { fallo = it.error != null && it.type != CameraState.Type.OPENING }
            estado.observe(cicloDeVida, observador)
            dejarDeObservar = { estado.removeObserver(observador) }
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir la cámara", e)
            fallo = true
        } finally {
            dejarDeObservar()
            proveedor?.unbind(*todos)
        }
    }
    Box(modifier) {
        AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize())
        if (fallo) AvisoCamaraNoAbre(alReintentar = { intento++ })
    }
}

/** La trasera, que es la que apunta a la etiqueta; si el celular solo tiene frontal, esa. */
private fun camaraDisponible(proveedor: ProcessCameraProvider): CameraSelector =
    if (proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA
    else CameraSelector.DEFAULT_FRONT_CAMERA

@Composable
private fun AvisoCamaraNoAbre(alReintentar: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(Espaciado.l),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No se pudo abrir la cámara. Inténtalo de nuevo.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            BotonSecundario(onClick = alReintentar) { Text("Reintentar") }
        }
    }
}

@VistaPrevia(name = "Cámara no abre · claro", showBackground = true, heightDp = 400)
@Composable
private fun AvisoClaro() = SigtecTheme { AvisoCamaraNoAbre(alReintentar = {}) }

@VistaPrevia(name = "Cámara no abre · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 400)
@Composable
private fun AvisoOscuro() = SigtecTheme(darkTheme = true) { AvisoCamaraNoAbre(alReintentar = {}) }
