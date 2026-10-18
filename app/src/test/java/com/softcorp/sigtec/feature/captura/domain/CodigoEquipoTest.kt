package com.softcorp.sigtec.feature.captura.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-09 (RF-903): cómo se limpia el texto leído antes de buscar el código del equipo. */
class CodigoEquipoTest {

    @Test
    fun `normalizar pasa el texto a mayusculas`() {
        assertEquals("PC-CONT-014", normalizarTexto("pc-cont-014"))
    }

    @Test
    fun `normalizar unifica los guiones que devuelve el reconocimiento`() {
        assertEquals("PC-CONT-014", normalizarTexto("PC–CONT—014"))
        assertEquals("IMP-RRHH-002", normalizarTexto("IMP‐RRHH−002"))
    }

    @Test
    fun `normalizar quita los espacios alrededor de los guiones`() {
        assertEquals("PC-CONT-014", normalizarTexto("pc - cont -  014"))
    }

    @Test
    fun `normalizar no toca los espacios lejos de un guion`() {
        assertEquals("SOFT CORPORATION · ACTIVO FIJO", normalizarTexto("Soft Corporation · Activo Fijo"))
    }

    @Test
    fun `de la etiqueta del prototipo 13 sale solo el codigo`() {
        val etiqueta = "SOFT CORPORATION · ACTIVO FIJO\nPC-CONT-014\nÁrea: Contabilidad"
        assertEquals(listOf("PC-CONT-014"), candidatosCodigo(etiqueta))
    }

    @Test
    fun `corrige letras y digitos confundidos segun la posicion`() {
        assertEquals(listOf("PC-CONT-014"), candidatosCodigo("PC-C0NT-O14"))
        assertEquals(listOf("IMP-RRHH-002"), candidatosCodigo("1MP-RRHH-OOZ"))
        assertEquals(listOf("PC-SIS-003"), candidatosCodigo("PC-5IS-OO3"))
    }

    @Test
    fun `acepta el codigo en minusculas y con espacios`() {
        assertEquals(listOf("PC-CONT-014"), candidatosCodigo("pc - cont - 014"))
    }

    @Test
    fun `fechas anexos y numeros sueltos no son codigos`() {
        assertEquals(emptyList<String>(), candidatosCodigo("15/10/2026 · 2026-10-15 · Anexo 2314 · 2026-10-152"))
    }

    @Test
    fun `no corta un codigo pegado a otro texto`() {
        assertEquals(emptyList<String>(), candidatosCodigo("XPC-CONT-014-A"))
    }

    @Test
    fun `varios codigos salen en orden de lectura y sin repetir`() {
        val texto = "PC-LOG-009 junto a PC-SIS-003 y otra vez PC-LOG-009"
        assertEquals(listOf("PC-LOG-009", "PC-SIS-003"), candidatosCodigo(texto))
    }

    @Test
    fun `sin codigo no hay candidatos`() {
        assertEquals(emptyList<String>(), candidatosCodigo("Encuadra la etiqueta del equipo dentro del marco"))
    }
}
