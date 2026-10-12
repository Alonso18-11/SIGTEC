package com.softcorp.sigtec.feature.asignacion.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-06 (RF-601, RF-605): textos de la pantalla de asignación. */
class TextosAsignacionTest {

    @Test
    fun `las iniciales usan las dos primeras palabras`() {
        assertEquals("LR", iniciales("Luis Ramírez"))
        assertEquals("MQ", iniciales("María Quispe Huamán"))
        assertEquals("A", iniciales("Ana"))
        assertEquals("JS", iniciales("  jorge   salas "))
    }

    @Test
    fun `el boton dice a quien se asigna`() {
        assertEquals("Asignar", textoBotonAsignar(null, esReasignacion = false))
        assertEquals("Asignar a Luis Ramírez", textoBotonAsignar("Luis Ramírez", esReasignacion = false))
    }

    @Test
    fun `si ya tenia tecnico el boton dice reasignar`() {
        assertEquals("Reasignar", textoBotonAsignar(null, esReasignacion = true))
        assertEquals("Reasignar a Luis Ramírez", textoBotonAsignar("Luis Ramírez", esReasignacion = true))
    }

    @Test
    fun `la linea bajo el boton no promete un aviso`() {
        assertEquals("La incidencia aparecerá en la bandeja de Luis Ramírez", textoBandeja("Luis Ramírez"))
    }
}
