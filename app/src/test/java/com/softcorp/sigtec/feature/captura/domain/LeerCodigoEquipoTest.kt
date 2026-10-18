package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.feature.captura.data.CatalogoEquiposRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-09 (RF-902 a RF-904, RF-907): el código que se ofrece después de leer la etiqueta. */
class LeerCodigoEquipoTest {

    private val catalogo = CatalogoEquiposRepositoryFalso()
    private val leerCodigo = LeerCodigoEquipo(catalogo)

    @Test
    fun `un codigo del catalogo se ofrece como conocido`() = runTest {
        val lectura = leerCodigo("SOFT CORPORATION · ACTIVO FIJO\nPC-C0NT-014\nÁrea: Contabilidad")
        assertEquals(LecturaCodigo("PC-CONT-014", enCatalogo = true), lectura)
    }

    @Test
    fun `un codigo fuera del catalogo se ofrece con el aviso`() = runTest {
        assertEquals(LecturaCodigo("LAP-GER-005", enCatalogo = false), leerCodigo("LAP-GER-005"))
    }

    @Test
    fun `con dos codigos gana el del catalogo aunque se lea despues`() = runTest {
        assertEquals(LecturaCodigo("PC-SIS-003", enCatalogo = true), leerCodigo("LAP-GER-005 / PC-SIS-003"))
        assertEquals(listOf("LAP-GER-005", "PC-SIS-003"), catalogo.consultados)
    }

    @Test
    fun `sin codigo no hay lectura ni consulta al catalogo`() = runTest {
        assertNull(leerCodigo("Encuadra la etiqueta del equipo dentro del marco"))
        assertTrue(catalogo.consultados.isEmpty())
    }
}
