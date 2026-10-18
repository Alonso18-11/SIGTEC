// El ExifInterface del sistema tuvo fallas antes de Android 7.1; con minSdk 26 no aplican y así no se agrega
// androidx.exifinterface como dependencia nueva (P-6)
@file:SuppressLint("ExifInterface")

package com.softcorp.sigtec.feature.captura.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.IOException

/**
 * HU-09 (P-6): decodifica la foto en [ruta] reducida a un lado mayor cercano a [ladoPx], sin librerías de imágenes,
 * y la endereza: CameraX guarda la orientación en el EXIF, no en los píxeles. Devuelve `null` si el archivo ya no
 * existe o no se puede leer (RF-935). Lee el disco: se llama fuera del hilo principal.
 */
fun decodificarReducida(ruta: String, ladoPx: Int): Bitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, medidas)
    if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

    // inSampleSize solo reduce en potencias de 2: la mayor que no deja la foto por debajo de ladoPx
    val ladoMayor = maxOf(medidas.outWidth, medidas.outHeight)
    var muestra = 1
    while (ladoMayor / (muestra * 2) >= ladoPx) muestra *= 2
    val foto = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra }) ?: return null

    val grados = gradosSegunExif(ruta)
    if (grados == 0) return foto
    val derecha = Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, Matrix().apply { postRotate(grados.toFloat()) }, true)
    if (derecha !== foto) foto.recycle()
    return derecha
}

private fun gradosSegunExif(ruta: String): Int {
    val orientacion = try {
        ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    return when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}
