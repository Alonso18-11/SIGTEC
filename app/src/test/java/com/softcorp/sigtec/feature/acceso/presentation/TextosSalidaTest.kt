package com.softcorp.sigtec.feature.acceso.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class TextosSalidaTest {

    @Test
    fun `un cambio usa singular`() {
        assertEquals("1 cambio sin enviar", textoCambiosSinEnviar(1))
    }

    @Test
    fun `varios cambios usan plural`() {
        assertEquals("2 cambios sin enviar", textoCambiosSinEnviar(2))
    }

    @Test
    fun `con un cambio el verbo va en singular`() {
        assertEquals("se perderá", textoSePerdera(1))
    }

    @Test
    fun `con varios cambios el verbo va en plural`() {
        assertEquals("se perderán", textoSePerdera(2))
    }
}
