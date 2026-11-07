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

    @Test
    fun `eliminar con la incidencia cerrada o reasignada muestra su propio aviso`() {
        assertEquals("Ya no puedes eliminar fotos de esta incidencia.", mensajeEliminarFoto(ErrorDominio.IncidenciaCerrada))
        assertEquals("Ya no puedes eliminar fotos de esta incidencia.", mensajeEliminarFoto(ErrorDominio.SinPermiso))
    }

    @Test
    fun `eliminar con otro error usa su texto de siempre`() {
        val error = ErrorDominio.Desconocido("disco lleno")
        assertEquals(error.mensaje(), mensajeEliminarFoto(error))
    }
}
