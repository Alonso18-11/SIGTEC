package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.falla
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.fuente
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.impresora
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.office
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.pantallaAzul
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.todas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-13 (RF-1304 a RF-1306): búsqueda y filtro del diccionario. */
class BuscarFallasTest {

    @Test
    fun `normalizar quita tildes y signos pero conserva la enie`() {
        assertEquals("cambio de ip en el año 2026", normalizar("¡Cambió de IP en el año 2026!"))
        assertEquals("año", normalizar("AÑO"))
        assertEquals("bateria lapiz", normalizar("  Batería,   lápiz. "))
    }

    @Test
    fun `sin texto ni tipo devuelve todas`() {
        assertEquals(todas, filtrarFallas(todas, "  ", null))
    }

    @Test
    fun `busca sin distinguir tildes ni mayusculas`() {
        assertEquals(listOf(impresora), filtrarFallas(todas, "IMPRESIÓN", null))
        assertEquals(listOf(fuente), filtrarFallas(todas, "estabilizador", null))
    }

    @Test
    fun `cada palabra puede estar en un campo distinto`() {
        // «office» en el componente y «cuenta» en la solución
        assertEquals(listOf(office), filtrarFallas(todas, "office cuenta", null))
        assertTrue(filtrarFallas(todas, "office estabilizador", null).isEmpty())
    }

    @Test
    fun `el filtro de tipo se combina con el texto`() {
        assertEquals(listOf(pantallaAzul, office), filtrarFallas(todas, "", TipoFalla.SOFTWARE))
        assertEquals(listOf(pantallaAzul), filtrarFallas(todas, "controlador", TipoFalla.SOFTWARE))
        assertTrue(filtrarFallas(todas, "controlador", TipoFalla.RED).isEmpty())
    }

    @Test
    fun `con 500 fallas filtra en menos de 3 segundos`() {
        val grandes = (1..500).map { falla("g$it", "Falla $it", "Componente $it", "Síntomas de la falla número $it") }
        val inicio = System.nanoTime()
        filtrarFallas(grandes, "síntomas número 250", TipoFalla.HARDWARE)
        assertTrue((System.nanoTime() - inicio) / 1e9 < 3)
    }
}
