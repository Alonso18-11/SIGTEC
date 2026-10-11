package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-05 (RF-80): «Entendido» descarta los cambios rechazados y el aviso deja de contarlos. */
class DescartarCambiosRechazadosTest {

    private val estadoDatos = EstadoDatosRepositoryFalso()
    private val descartar = DescartarCambiosRechazados(estadoDatos)

    @Test
    fun `descartar deja en cero los cambios rechazados`() = runTest {
        estadoDatos.fijarNecesitanAtencion(2)

        val resultado = descartar()

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(0, estadoDatos.estado.first().necesitanAtencion)
    }

    @Test
    fun `si Room falla devuelve el error y los rechazos siguen`() = runTest {
        estadoDatos.fijarNecesitanAtencion(1)
        estadoDatos.fallar = true

        val resultado = descartar()

        assertTrue(resultado is Resultado.Error)
        assertEquals(1, estadoDatos.estado.first().necesitanAtencion)
    }
}
