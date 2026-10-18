// El ExifInterface del sistema tuvo fallas antes de Android 7.1; con minSdk 26 no aplican y así no se agrega
// androidx.exifinterface como dependencia nueva (P-6)
@file:SuppressLint("ExifInterface")

package com.softcorp.sigtec.feature.captura.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Lo que se ve de una foto mientras se lee del disco. */
internal sealed interface CargaFoto {
    data object Cargando : CargaFoto
    data object NoDisponible : CargaFoto
    data class Lista(val imagen: ImageBitmap) : CargaFoto
}

/**
 * HU-09: una foto de evidencia en un cuadrado de [lado], recortada para llenarlo. Se decodifica reducida y fuera
 * del hilo principal; si el archivo ya no existe o no se puede leer, muestra «Foto no disponible» (RF-935).
 */
@Composable
fun MiniaturaEvidencia(ruta: String, lado: Dp, descripcion: String?, modifier: Modifier = Modifier) {
    val ladoPx = with(LocalDensity.current) { lado.roundToPx() }
    val carga by produceState<CargaFoto>(CargaFoto.Cargando, ruta, ladoPx) {
        // P-6: un composable sin prueba JVM, así que no necesita un despachador inyectado
        value = withContext(Dispatchers.IO) { decodificarReducida(ruta, ladoPx) }
            ?.let { CargaFoto.Lista(it.asImageBitmap()) }
            ?: CargaFoto.NoDisponible
    }
    ContenidoMiniatura(carga, lado, descripcion, modifier)
}

@Composable
internal fun ContenidoMiniatura(carga: CargaFoto, lado: Dp, descripcion: String?, modifier: Modifier = Modifier) {
    val forma = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .size(lado)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma),
        contentAlignment = Alignment.Center
    ) {
        when (carga) {
            CargaFoto.Cargando -> Unit
            CargaFoto.NoDisponible -> Text(
                "Foto no disponible",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Espaciado.xs)
            )
            is CargaFoto.Lista -> Image(
                carga.imagen,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(lado)
            )
        }
    }
}

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

@Preview(name = "Miniaturas · claro", showBackground = true)
@Composable
private fun MiniaturasClaro() = SigtecTheme { Surface { Previa() } }

@Preview(name = "Miniaturas · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MiniaturasOscuro() = SigtecTheme(darkTheme = true) { Surface { Previa() } }

@Composable
private fun Previa() = Row(Modifier.padding(Espaciado.m)) {
    ContenidoMiniatura(CargaFoto.Cargando, 64.dp, null)
    ContenidoMiniatura(CargaFoto.NoDisponible, 64.dp, null, Modifier.padding(start = Espaciado.s))
}
