package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-10 (RF-1003, RF-1015 a RF-1018): los textos que ve la persona coinciden con la spec. */
class MensajesDictadoTest {

    @Test
    fun `cada error tiene el texto de la spec`() {
        assertEquals("No se entendió. Toca el micrófono e inténtalo de nuevo.", mensajeDictado(ErrorDictado.SIN_ENTENDER))
        assertEquals(
            "Sin conexión, este celular no puede reconocer la voz. Conéctate o escribe el texto en el formulario.",
            mensajeDictado(ErrorDictado.SIN_CONEXION)
        )
        assertEquals(
            "Este celular no tiene reconocimiento de voz. Escribe el texto en el formulario.",
            mensajeDictado(ErrorDictado.SIN_SERVICIO)
        )
        assertEquals(
            "Sin el permiso de micrófono no se puede dictar. Puedes escribir el texto en el formulario.",
            mensajeDictado(ErrorDictado.SIN_PERMISO)
        )
        assertEquals("No se pudo usar el micrófono. Inténtalo de nuevo.", mensajeDictado(ErrorDictado.MICROFONO))
    }

    @Test
    fun `el titulo nombra el campo que se dicta`() {
        assertEquals("Dictar descripción", tituloDictado(CampoDictado.DESCRIPCION))
        assertEquals("Dictar diagnóstico", tituloDictado(CampoDictado.DIAGNOSTICO))
        assertEquals("Dictar solución aplicada", tituloDictado(CampoDictado.SOLUCION))
        assertEquals("Dictar observaciones", tituloDictado(CampoDictado.OBSERVACIONES))
    }
}
