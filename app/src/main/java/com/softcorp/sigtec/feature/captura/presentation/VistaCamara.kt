package com.softcorp.sigtec.feature.captura.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation

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

    LaunchedEffect(casos, cicloDeVida) {
        val proveedor = ProcessCameraProvider.awaitInstance(contexto)
        val todos = arrayOf(previa, *casos.toTypedArray())
        try {
            previa.setSurfaceProvider(vista.surfaceProvider)
            proveedor.bindToLifecycle(cicloDeVida, camaraDisponible(proveedor), *todos)
            awaitCancellation()
        } finally {
            proveedor.unbind(*todos)
        }
    }
    AndroidView(factory = { vista }, modifier = modifier)
}

/** La trasera, que es la que apunta a la etiqueta; si el celular solo tiene frontal, esa. */
private fun camaraDisponible(proveedor: ProcessCameraProvider): CameraSelector =
    if (proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA
    else CameraSelector.DEFAULT_FRONT_CAMERA
