package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.data.PreferenciasSeguridadFalsas
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-01: ingresar con otra cuenta mientras hay un dueño bloqueado (RF-11, RF-13). */
class EvaluarIngresoDeOtraPersonaTest {

    private val sesion = SesionRepositoryDemo()
    private val preferencias = PreferenciasSeguridadFalsas()
    private val bloqueo = BloqueoApp()
    private val estadoDatos = EstadoDatosRepositoryFalso()

    private val cambiarHuella = CambiarIngresoConHuella(sesion, preferencias, bloqueo)
    private val evaluar = EvaluarIngresoDeOtraPersona(
        ObservarEstadoAcceso(sesion, preferencias, bloqueo),
        ObservarEstadoDatos(estadoDatos)
    )

    // deja a Luis como dueño bloqueado, igual que en `EstadoAccesoTest`
    private suspend fun luisBloqueado() {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        cambiarHuella(true)
        bloqueo.bloquear()
    }

    @Test
    fun `sin dueno bloqueado no hay riesgo`() = runTest {
        assertEquals(DecisionBorrado.SinRiesgo, evaluar("atorres@softcorp.pe"))
    }

    @Test
    fun `el mismo dueno no hay riesgo`() = runTest {
        luisBloqueado()
        estadoDatos.fijarPendientes(3)
        assertEquals(DecisionBorrado.SinRiesgo, evaluar("lramirez@softcorp.pe"))
    }

    @Test
    fun `otra persona sin pendientes no hay riesgo`() = runTest {
        luisBloqueado()
        estadoDatos.fijarPendientes(0)
        assertEquals(DecisionBorrado.SinRiesgo, evaluar("atorres@softcorp.pe"))
    }

    @Test
    fun `otra persona con pendientes pide confirmar`() = runTest {
        luisBloqueado()
        estadoDatos.fijarPendientes(2)
        assertEquals(DecisionBorrado.Confirmar(2), evaluar("atorres@softcorp.pe"))
    }

    @Test
    fun `otra persona con solo un cambio rechazado pide confirmar`() = runTest {
        // RF-81 (HU-05)
        luisBloqueado()
        estadoDatos.fijarPendientes(0)
        estadoDatos.fijarNecesitanAtencion(1)
        assertEquals(DecisionBorrado.Confirmar(1), evaluar("atorres@softcorp.pe"))
    }
}
