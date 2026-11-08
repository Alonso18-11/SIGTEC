package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.feature.atencion.domain.Disponibilidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-11 (RF-1105 a RF-1108): los textos de la disponibilidad coinciden con la spec. */
class TextosRepuestoTest {

    @Test
    fun `el envio va en singular con un dia`() {
        assertEquals("envío estimado en 1 día", textoEnvio(1))
        assertEquals("envío estimado en 3 días", textoEnvio(3))
    }

    @Test
    fun `con stock dice las unidades y el envio`() {
        val texto = textoDisponibilidad(Disponibilidad.ConStock(3, 1))!!
        assertEquals("Disponible en almacén: 3 unidades", texto.titulo)
        assertEquals("Consultado a Logística hace un momento, envío estimado en 1 día", texto.detalle)
        assertEquals("Disponible en almacén: 1 unidad", textoDisponibilidad(Disponibilidad.ConStock(1, 2))!!.titulo)
    }

    @Test
    fun `sin stock avisa que Logistica debera conseguirla`() {
        assertEquals("Sin stock en almacén, Logística deberá conseguirla", textoDisponibilidad(Disponibilidad.SinStock(5))!!.titulo)
    }

    @Test
    fun `sin coincidencias y sin respuesta dejan pedir igual`() {
        assertEquals(
            "Puedes solicitarla igual y describirla en el motivo.",
            textoDisponibilidad(Disponibilidad.NoEnCatalogo)!!.detalle
        )
        assertEquals(
            "Puedes solicitarla igual; la disponibilidad se confirmará después.",
            textoDisponibilidad(Disponibilidad.SinRespuesta)!!.detalle
        )
    }

    @Test
    fun `mientras consulta no hay texto`() {
        assertNull(textoDisponibilidad(Disponibilidad.Consultando))
    }

    @Test
    fun `los dias de espera se atribuyen a Logistica`() {
        assertEquals("1 día de espera, la demora se atribuye a Logística", textoDiasEspera(1))   // RF-1122
        assertEquals("4 días de espera, la demora se atribuye a Logística", textoDiasEspera(4))
    }
}
