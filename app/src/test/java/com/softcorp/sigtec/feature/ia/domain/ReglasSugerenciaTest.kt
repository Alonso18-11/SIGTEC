package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-14 (RF-1413, D-4): cuándo una clasificación sugerida cuenta como aceptada. */
class ReglasSugerenciaTest {

    private val sugerida = ClasificacionSugerida(TipoFalla.RED, Prioridad.MEDIA)

    @Test
    fun `usada y asignada sin cambios cuenta como aceptada`() {
        assertTrue(clasificacionAceptada(sugerida, usada = true, descartada = false, TipoFalla.RED, Prioridad.MEDIA))
    }

    @Test
    fun `usada y corregida no cuenta`() {
        assertFalse(clasificacionAceptada(sugerida, usada = true, descartada = false, TipoFalla.RED, Prioridad.ALTA))
        assertFalse(clasificacionAceptada(sugerida, usada = true, descartada = false, TipoFalla.HARDWARE, Prioridad.MEDIA))
        assertFalse(clasificacionAceptada(sugerida, usada = true, descartada = false, null, Prioridad.MEDIA))
    }

    @Test
    fun `descartada no cuenta aunque se elija lo mismo a mano`() {
        assertFalse(clasificacionAceptada(sugerida, usada = false, descartada = true, TipoFalla.RED, Prioridad.MEDIA))
    }

    @Test
    fun `asignada sin usarla no cuenta`() {
        assertFalse(clasificacionAceptada(sugerida, usada = false, descartada = false, TipoFalla.RED, Prioridad.MEDIA))
        assertFalse(clasificacionAceptada(sugerida, usada = false, descartada = false, null, null))
    }
}
