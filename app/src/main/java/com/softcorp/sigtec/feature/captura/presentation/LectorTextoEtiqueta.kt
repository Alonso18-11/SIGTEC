package com.softcorp.sigtec.feature.captura.presentation

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

private const val TAG = "LectorTextoEtiqueta"

/**
 * Lee el texto de cada imagen de la cámara con ML Kit, dentro del celular y sin conexión (RF-902), y lo
 * entrega a [alLeerTexto]; buscar el código es trabajo de `LeerCodigoEquipo`. No guarda ni envía las
 * imágenes ni el texto (RF-914). Quien lo crea lo cierra con [close] al salir de la pantalla.
 */
class LectorTextoEtiqueta(private val alLeerTexto: (String) -> Unit) : ImageAnalysis.Analyzer, AutoCloseable {

    private val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imagen: ImageProxy) {
        val medio = imagen.image
        if (medio == null) {
            imagen.close()
            return
        }
        reconocedor.process(InputImage.fromMediaImage(medio, imagen.imageInfo.rotationDegrees))
            .addOnSuccessListener { texto -> alLeerTexto(texto.text) }
            .addOnFailureListener { e -> Log.w(TAG, "No se pudo leer el texto de la imagen", e) }
            // CameraX no entrega la siguiente imagen hasta cerrar esta (P-3)
            .addOnCompleteListener { imagen.close() }
    }

    override fun close() = reconocedor.close()
}
