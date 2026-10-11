package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-01: sin pendientes no hay riesgo; con pendientes hay que confirmar (RF-13, RF-14). */
class EvaluarCierreSesionTest {

    private val estadoDatos = EstadoDatosRepositoryFalso()
    private val evaluar = EvaluarCierreSesion(ObservarEstadoDatos(estadoDatos))

    @Test
    fun `sin pendientes no hay riesgo`() = runTest {
        estadoDatos.fijarPendientes(0)
        assertEquals(DecisionBorrado.SinRiesgo, evaluar())
    }

    @Test
    fun `con pendientes pide confirmar`() = runTest {
        estadoDatos.fijarPendientes(3)
        assertEquals(DecisionBorrado.Confirmar(3), evaluar())
    }

    @Test
    fun `sin pendientes pero con un cambio rechazado pide confirmar`() = runTest {
        // RF-81 (HU-05)
        estadoDatos.fijarPendientes(0)
        estadoDatos.fijarNecesitanAtencion(1)
        assertEquals(DecisionBorrado.Confirmar(1), evaluar())
    }

    @Test
    fun `suma los pendientes y los rechazados`() = runTest {
        estadoDatos.fijarPendientes(2)
        estadoDatos.fijarNecesitanAtencion(1)
        assertEquals(DecisionBorrado.Confirmar(3), evaluar())
    }
}
