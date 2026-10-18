package com.softcorp.sigtec.feature.captura.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-09 (RF-905, RF-908): un código se da por reconocido con 3 lecturas iguales seguidas. */
class RachaLecturaTest {

    private fun leer(vararg lecturas: String?): RachaLectura =
        lecturas.fold(RachaLectura()) { racha, lectura -> racha.siguiente(lectura) }

    @Test
    fun `tres lecturas iguales seguidas reconocen el codigo`() {
        val racha = leer("PC-CONT-014", "PC-CONT-014", "PC-CONT-014")
        assertTrue(racha.estable)
        assertEquals("PC-CONT-014", racha.codigo)
    }

    @Test
    fun `dos lecturas iguales todavia no alcanzan`() {
        assertFalse(leer("PC-CONT-014", "PC-CONT-014").estable)
    }

    @Test
    fun `otro codigo corta la racha y empieza en uno`() {
        val racha = leer("PC-CONT-014", "PC-CONT-014", "PC-LOG-009")
        assertFalse(racha.estable)
        assertEquals(RachaLectura("PC-LOG-009", 1), racha)
    }

    @Test
    fun `una imagen sin codigo deja la racha vacia`() {
        val racha = leer("PC-CONT-014", "PC-CONT-014", null)
        assertEquals(RachaLectura(), racha)
        assertFalse(leer("PC-CONT-014", "PC-CONT-014", null, "PC-CONT-014").estable)
    }

    @Test
    fun `una racha nueva empieza vacia y sin reconocer`() {
        assertFalse(RachaLectura().estable)
        assertEquals(RachaLectura(null, 0), RachaLectura())
    }
}
