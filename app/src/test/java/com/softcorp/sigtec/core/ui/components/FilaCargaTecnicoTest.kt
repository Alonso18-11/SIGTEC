package com.softcorp.sigtec.core.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-06: la fila del técnico se bloquea al llegar a su límite, igual que la regla del dominio. */
class FilaCargaTecnicoTest {

    @Test
    fun `por debajo del limite se puede elegir`() {
        assertFalse(alcanzoLimite(activas = 3, limite = 4))
    }

    @Test
    fun `en el limite o por encima queda bloqueada`() {
        assertTrue(alcanzoLimite(activas = 4, limite = 4))
        assertTrue(alcanzoLimite(activas = 5, limite = 4))
    }
}
