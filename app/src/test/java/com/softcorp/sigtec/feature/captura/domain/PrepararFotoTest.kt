package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-09 (RF-925): cada foto tiene su id y su ruta dentro de la carpeta de la incidencia. */
class PrepararFotoTest {

    private val repo = EvidenciaRepositoryFalso()
    private val preparar = PrepararFoto(repo)

    @Test
    fun `dos fotos seguidas tienen ids distintos`() = runTest {
        val primera = preparar("i124")
        val segunda = preparar("i124")

        assertNotEquals(primera.id, segunda.id)
        assertNotEquals(primera.ruta, segunda.ruta)
    }

    @Test
    fun `la ruta es la que da el repositorio para esa incidencia y ese id`() = runTest {
        val foto = preparar("i124")

        assertEquals(repo.rutaParaNuevaFoto("i124", foto.id), foto.ruta)
        assertTrue(foto.ruta.contains("/i124/"))
    }

    @Test
    fun `preparar no enlaza ninguna foto`() = runTest {
        preparar("i124")

        assertTrue(repo.evidencias.isEmpty())   // eso lo hace GuardarEvidencia, ya con la foto escrita
    }
}
