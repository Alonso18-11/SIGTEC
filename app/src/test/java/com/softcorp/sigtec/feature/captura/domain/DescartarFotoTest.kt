package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-09 (RF-928): la foto que no se enlazó no queda en el celular. */
class DescartarFotoTest {

    private val repo = EvidenciaRepositoryFalso()
    private val descartar = DescartarFoto(repo)

    @Test
    fun `borra el archivo de la foto y no enlaza nada`() = runTest {
        val foto = FotoNueva("f1", "/evidencias/i124/f1.jpg")

        descartar(foto)

        assertEquals(listOf(foto.ruta), repo.descartadas)
        assertTrue(repo.evidencias.isEmpty())
    }
}
