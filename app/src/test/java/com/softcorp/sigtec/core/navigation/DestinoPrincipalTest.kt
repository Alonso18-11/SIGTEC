package com.softcorp.sigtec.core.navigation

import com.softcorp.sigtec.core.domain.model.Perfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-1, RF-8: tabla perfil -> menú de la sección 3.1 del plan.
class DestinoPrincipalTest {

    @Test
    fun `jefe ve inicio incidencias e indicadores`() {
        assertEquals(
            listOf(
                DestinoPrincipal.INICIO,
                DestinoPrincipal.INCIDENCIAS,
                DestinoPrincipal.INDICADORES
            ),
            DestinoPrincipal.para(Perfil.JEFE)
        )
    }

    @Test
    fun `tecnico ve inicio incidencias mis tareas y diccionario`() {
        assertEquals(
            listOf(
                DestinoPrincipal.INICIO,
                DestinoPrincipal.INCIDENCIAS,
                DestinoPrincipal.MIS_TAREAS,
                DestinoPrincipal.DICCIONARIO
            ),
            DestinoPrincipal.para(Perfil.TECNICO)
        )
    }

    @Test
    fun `personal de sistemas ve inicio incidencias y diccionario`() {
        assertEquals(
            listOf(
                DestinoPrincipal.INICIO,
                DestinoPrincipal.INCIDENCIAS,
                DestinoPrincipal.DICCIONARIO
            ),
            DestinoPrincipal.para(Perfil.PERSONAL_SISTEMAS)
        )
    }

    @Test
    fun `el jefe no ve diccionario`() {
        assertFalse(DestinoPrincipal.para(Perfil.JEFE).contains(DestinoPrincipal.DICCIONARIO))
    }

    @Test
    fun `solo el tecnico ve mis tareas`() {
        assertTrue(DestinoPrincipal.para(Perfil.TECNICO).contains(DestinoPrincipal.MIS_TAREAS))
        assertFalse(DestinoPrincipal.para(Perfil.JEFE).contains(DestinoPrincipal.MIS_TAREAS))
        assertFalse(DestinoPrincipal.para(Perfil.PERSONAL_SISTEMAS).contains(DestinoPrincipal.MIS_TAREAS))
    }
}
