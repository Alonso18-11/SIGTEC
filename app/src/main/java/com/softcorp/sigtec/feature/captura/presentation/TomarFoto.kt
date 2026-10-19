package com.softcorp.sigtec.feature.captura.presentation

import android.util.Log
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private const val TAG = "TomarFoto"

// P-5: la foto sale ya reducida y comprimida (unos 300 a 500 KB), sin decodificar ni recomprimir en el celular
private val TAMANO_FOTO = Size(1600, 1200)
private const val CALIDAD_JPEG = 85

/** El caso de uso de CameraX para «Evidencia fotográfica», que se pasa a `VistaCamara`. */
fun crearCapturaDeFoto(): ImageCapture = ImageCapture.Builder()
    .setResolutionSelector(
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(TAMANO_FOTO, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
            )
            .build()
    )
    .setJpegQuality(CALIDAD_JPEG)
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()

/**
 * HU-09 (RF-925): toma una foto y la escribe en [ruta] (la de `PrepararFoto`). Devuelve `false` si la cámara o la
 * escritura fallan; entonces quien llama descarta el archivo (RF-928). No guarda nada en Room.
 */
suspend fun ImageCapture.tomarFoto(ruta: String): Boolean = suspendCancellableCoroutine { continuacion ->
    val opciones = ImageCapture.OutputFileOptions.Builder(File(ruta)).build()
    // El aviso llega en un hilo de CameraX; solo reanuda la corrutina, así que no hace falta otro ejecutor
    takePicture(opciones, Executor(Runnable::run), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
            continuacion.resume(true)
        }

        override fun onError(e: ImageCaptureException) {
            Log.w(TAG, "No se pudo tomar la foto", e)
            continuacion.resume(false)
        }
    })
}
