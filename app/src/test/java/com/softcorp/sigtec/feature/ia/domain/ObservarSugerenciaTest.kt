package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.ia.data.AsistenteIAFalso
import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-14 (RF-1405, RF-1415, RF-1424): la sugerencia a la vista y si la IA está disponible. */
class ObservarSugerenciaTest {

    private val repo = SugerenciaIARepositoryFalso()
    private val asistente = AsistenteIAFalso()
    private val t0 = Instant.parse("2026-11-16T15:00:00Z")

    @Test
    fun `muestra la mas reciente sin decidir de ese tipo`() = runTest {
        repo.cargar(
            SugerenciaIA("b1", "i124", TipoSugerencia.BORRADOR_INFORME, "{}", "m", t0),
            SugerenciaIA("b2", "i124", TipoSugerencia.BORRADOR_INFORME, "{}", "m", t0.plusSeconds(60)),
            SugerenciaIA("b3", "i124", TipoSugerencia.BORRADOR_INFORME, "{}", "m", t0.plusSeconds(120), aceptada = false)
        )
        val observar = ObservarSugerencia(repo)

        assertEquals("b2", observar("i124", TipoSugerencia.BORRADOR_INFORME).first()?.id)
        assertNull(observar("i124", TipoSugerencia.CLASIFICACION).first())
    }

    @Test
    fun `la disponibilidad sigue la conexion`() = runTest {
        val disponible = ObservarIADisponible(asistente)
        assertEquals(true, disponible().first())

        asistente.disponible.value = false

        assertFalse(disponible().first())
    }
}
