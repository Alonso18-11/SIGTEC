package com.softcorp.sigtec.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Retema v2 (plan, sección 5.1): los textos de la etiqueta, sin punto medio y con el día en singular. */
class EtiquetaIncidenciaTest {

    @Test
    fun `critico lleva los dias con coma`() {
        assertEquals("Crítico, 6 días", textoCritico(6))
        assertEquals("Crítico, 1 día", textoCritico(1))
    }

    @Test
    fun `esperando repuesto lleva los dias con coma`() {
        assertEquals("Esperando repuesto, 4 días", textoEsperandoRepuesto(4))
        assertEquals("Esperando repuesto, 0 días", textoEsperandoRepuesto(0))
        assertEquals("Esperando repuesto, 1 día", textoEsperandoRepuesto(1))
    }
}
