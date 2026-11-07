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

    @Test
    fun `una foto usa singular y varias plural`() {
        assertEquals("1 foto de evidencia", textoFotosEvidencia(1))
        assertEquals("3 fotos de evidencia", textoFotosEvidencia(3))
    }

    @Test
    fun `salida solo con cambios queda como RF-13`() {
        assertEquals("Tienes 1 cambio sin enviar. Si cierras sesión ahora, se perderá.", textoAvisoSalida(1, 0))
        assertEquals("Tienes 2 cambios sin enviar. Si cierras sesión ahora, se perderán.", textoAvisoSalida(2, 0))
    }

    @Test
    fun `salida solo con fotos`() {
        assertEquals(
            "Tienes 1 foto de evidencia en este celular. Si cierras sesión ahora, se perderá.",
            textoAvisoSalida(0, 1)
        )
        assertEquals(
            "Tienes 4 fotos de evidencia en este celular. Si cierras sesión ahora, se perderán.",
            textoAvisoSalida(0, 4)
        )
    }

    @Test
    fun `salida con cambios y fotos va en plural`() {
        assertEquals(
            "Tienes 1 cambio sin enviar y 1 foto de evidencia en este celular. Si cierras sesión ahora, se perderán.",
            textoAvisoSalida(1, 1)
        )
    }

    @Test
    fun `ingreso solo con cambios queda como RF-11`() {
        assertEquals(
            "Luis tiene 1 cambio sin enviar en este celular. Si ingresas, se perderá.",
            textoAvisoIngreso("Luis", 1, 0)
        )
    }

    @Test
    fun `ingreso solo con fotos`() {
        assertEquals(
            "Luis tiene 1 foto de evidencia en este celular. Si ingresas, se perderá.",
            textoAvisoIngreso("Luis", 0, 1)
        )
    }

    @Test
    fun `ingreso con cambios y fotos`() {
        assertEquals(
            "Luis tiene 2 cambios sin enviar y 3 fotos de evidencia en este celular. Si ingresas, se perderán.",
            textoAvisoIngreso("Luis", 2, 3)
        )
    }
}
