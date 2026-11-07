package com.softcorp.sigtec.feature.captura.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-10 (RF-1008, RF-1011): lo dictado se agrega al final sin tocar lo que ya había. */
class TextoDictadoTest {

    @Test
    fun `en un campo vacio queda solo lo dictado`() {
        assertEquals("No enciende", unirTexto("", "No enciende"))
    }

    @Test
    fun `un campo con solo espacios se reemplaza`() {
        assertEquals("No enciende", unirTexto("   ", "No enciende"))
    }

    @Test
    fun `con texto se agrega al final separado por un espacio`() {
        assertEquals("No enciende. Huele a quemado", unirTexto("No enciende.", "Huele a quemado"))
    }

    @Test
    fun `si el campo termina en espacio no se agrega otro`() {
        assertEquals("No enciende. Huele a quemado", unirTexto("No enciende. ", "Huele a quemado"))
    }

    @Test
    fun `si el campo termina en salto de linea se respeta`() {
        assertEquals("No enciende.\nHuele a quemado", unirTexto("No enciende.\n", "Huele a quemado"))
    }

    @Test
    fun `se quitan los espacios de los extremos de lo dictado`() {
        assertEquals("No enciende huele a quemado", unirTexto("No enciende", "  huele a quemado  "))
    }

    @Test
    fun `un dictado vacio o con solo espacios no cambia el campo`() {
        assertEquals("No enciende", unirTexto("No enciende", ""))
        assertEquals("No enciende ", unirTexto("No enciende ", "   "))
    }
}
