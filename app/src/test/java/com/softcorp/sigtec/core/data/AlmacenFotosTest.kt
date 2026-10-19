package com.softcorp.sigtec.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** HU-09 (RF-928, RF-940, RF-942): la carpeta interna de las fotos, sobre una carpeta temporal. */
class AlmacenFotosTest {

    @get:Rule
    val temporal = TemporaryFolder()

    private val raiz by lazy { File(temporal.root, "evidencias") }
    private val almacen by lazy { AlmacenFotos(raiz) }

    @Test
    fun `archivoNuevo crea la carpeta de la incidencia dentro de la raiz`() {
        val archivo = almacen.archivoNuevo("inc-1", "foto-1")

        assertEquals(File(File(raiz, "inc-1"), "foto-1.jpg"), archivo)
        assertTrue(archivo.parentFile!!.isDirectory)
        assertFalse(archivo.exists())   // la foto la escribe la cámara, no el almacén
    }

    @Test
    fun `borrar quita la foto y no falla si ya no existe`() {
        val archivo = almacen.archivoNuevo("inc-1", "foto-1").apply { writeText("jpg") }

        almacen.borrar(archivo.absolutePath)
        assertFalse(archivo.exists())

        almacen.borrar(archivo.absolutePath)   // segunda vez: no lanza
    }

    @Test
    fun `borrar no toca archivos fuera de la carpeta de fotos`() {
        val ajeno = temporal.newFile("sigtec.db")

        almacen.borrar(ajeno.absolutePath)
        almacen.borrar(File(raiz, "../sigtec.db").path)

        assertTrue(ajeno.exists())
    }

    @Test
    fun `borrarTodo vacia la carpeta de fotos y deja lo demas`() {
        val foto1 = almacen.archivoNuevo("inc-1", "foto-1").apply { writeText("jpg") }
        val foto2 = almacen.archivoNuevo("inc-2", "foto-2").apply { writeText("jpg") }
        val ajeno = temporal.newFile("sigtec.db")

        almacen.borrarTodo()

        assertFalse(foto1.exists())
        assertFalse(foto2.exists())
        assertFalse(raiz.exists())
        assertTrue(ajeno.exists())
    }

    @Test
    fun `despues de borrarTodo se pueden guardar fotos otra vez`() {
        almacen.archivoNuevo("inc-1", "foto-1").writeText("jpg")
        almacen.borrarTodo()

        val archivo = almacen.archivoNuevo("inc-1", "foto-2")

        assertTrue(archivo.parentFile!!.isDirectory)
    }
}
