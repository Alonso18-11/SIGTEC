package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.ui.mensaje
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-09 (RF-929, RF-931): los avisos de la captura de fotos. */
class MensajesFotosTest {

    @Test
    fun `cerrada o reasignada muestra el aviso propio de las fotos`() {
        assertEquals("Ya no puedes agregar fotos a esta incidencia.", mensajeFotos(ErrorDominio.IncidenciaCerrada))
        assertEquals("Ya no puedes agregar fotos a esta incidencia.", mensajeFotos(ErrorDominio.SinPermiso))
    }

    @Test
    fun `el maximo de fotos usa su mensaje`() {
        assertEquals("Llegaste al máximo de 10 fotos por incidencia.", mensajeFotos(ErrorDominio.LimiteFotosAlcanzado))
    }

    @Test
    fun `los demas errores usan su texto de siempre`() {
        val error = ErrorDominio.Desconocido("disco lleno")
        assertEquals(error.mensaje(), mensajeFotos(error))
    }
}
