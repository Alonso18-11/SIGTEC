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
}
