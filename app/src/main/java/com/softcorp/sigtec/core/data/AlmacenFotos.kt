package com.softcorp.sigtec.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-09: las fotos de evidencia viven solo en este celular, en `filesDir/evidencias/<incidenciaId>/<id>.jpg`
 * (RF-940, P-4). `filesDir` es privado de la app y Android no lo vacía solo, a diferencia de `cacheDir`.
 * Hace E/S de disco: quien lo usa lo llama desde `@IoDispatcher`.
 */
@Singleton
class AlmacenFotos(private val raiz: File) {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(File(contexto.filesDir, CARPETA))

    /** El archivo donde se guardará la foto [id]; crea la carpeta de la incidencia, no el archivo (RF-925). */
    fun archivoNuevo(incidenciaId: String, id: String): File =
        File(raiz, incidenciaId).apply { mkdirs() }.resolve("$id.jpg")

    /** Borra una foto; si ya no existe no pasa nada. Nunca toca archivos fuera de la carpeta de fotos (RF-928). */
    fun borrar(ruta: String) {
        val archivo = File(ruta).canonicalFile
        if (archivo.startsWith(raiz.canonicalFile)) archivo.delete()
    }

    /** Borra todas las fotos, junto con los datos locales de la sesión (RF-942, D-7). */
    fun borrarTodo() {
        raiz.deleteRecursively()
    }

    private companion object {
        const val CARPETA = "evidencias"
    }
}
