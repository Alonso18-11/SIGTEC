package com.softcorp.sigtec.core.domain

import com.softcorp.sigtec.core.domain.model.Perfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasSesionTest {

    @Test
    fun `sin cambios pendientes no hay riesgo`() {
        assertEquals(DecisionBorrado.SinRiesgo, evaluarBorrado(0))
    }

    @Test
    fun `un cambio pendiente pide confirmar en singular`() {
        assertEquals(DecisionBorrado.Confirmar(1), evaluarBorrado(1))
    }

    @Test
    fun `varios cambios pendientes piden confirmar con el total`() {
        assertEquals(DecisionBorrado.Confirmar(5), evaluarBorrado(5))
    }

    @Test
    fun `normalizarCorreo quita espacios de los extremos`() {
        assertEquals("correo@softcorp.pe", normalizarCorreo("  correo@softcorp.pe  "))
    }

    @Test
    fun `normalizarCorreo convierte mayusculas a minusculas`() {
        assertEquals("correo@softcorp.pe", normalizarCorreo("Correo@Softcorp.PE"))
    }

    @Test
    fun `normalizarCorreo no toca espacios internos`() {
        assertEquals("a b@softcorp.pe", normalizarCorreo("  a b@softcorp.pe  "))
    }

    @Test
    fun `perfilDesdeTexto reconoce JEFE`() {
        assertEquals(Perfil.JEFE, perfilDesdeTexto("JEFE"))
    }

    @Test
    fun `perfilDesdeTexto reconoce TECNICO`() {
        assertEquals(Perfil.TECNICO, perfilDesdeTexto("TECNICO"))
    }

    @Test
    fun `perfilDesdeTexto reconoce PERSONAL_SISTEMAS`() {
        assertEquals(Perfil.PERSONAL_SISTEMAS, perfilDesdeTexto("PERSONAL_SISTEMAS"))
    }

    @Test
    fun `perfilDesdeTexto con texto nulo devuelve null`() {
        assertNull(perfilDesdeTexto(null))
    }

    @Test
    fun `perfilDesdeTexto con texto mal escrito devuelve null`() {
        assertNull(perfilDesdeTexto("TECNIC0"))
    }

    @Test
    fun `perfilDesdeTexto no ignora mayusculas`() {
        assertNull(perfilDesdeTexto("tecnico"))
    }

    @Test
    fun `debeBorrarDatosAlEntrar con anterior nulo no borra`() {
        assertEquals(false, debeBorrarDatosAlEntrar(null, "uid-nuevo"))
    }

    @Test
    fun `debeBorrarDatosAlEntrar con el mismo uid no borra`() {
        assertEquals(false, debeBorrarDatosAlEntrar("uid-1", "uid-1"))
    }

    @Test
    fun `debeBorrarDatosAlEntrar con uid distinto borra`() {
        assertEquals(true, debeBorrarDatosAlEntrar("uid-1", "uid-2"))
    }

    @Test
    fun `esOtraPersona sin dueno bloqueado devuelve false`() {
        assertEquals(false, esOtraPersona(null, "juan@softcorp.pe"))
    }

    @Test
    fun `esOtraPersona con el mismo correo en otro formato devuelve false`() {
        assertEquals(false, esOtraPersona("juan@softcorp.pe", "Juan@Softcorp.PE"))
    }

    @Test
    fun `esOtraPersona con otro correo devuelve true`() {
        assertEquals(true, esOtraPersona("juan@softcorp.pe", "ana@softcorp.pe"))
    }
}
