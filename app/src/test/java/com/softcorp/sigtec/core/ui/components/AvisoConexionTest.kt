package com.softcorp.sigtec.core.ui.components

import com.softcorp.sigtec.core.domain.model.EstadoDatos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-05 (RF-76, RF-77): `textoAviso` decide qué mensaje mostrar, sin Compose de por medio. */
class AvisoConexionTest {

    @Test
    fun `sin conexion y sin descarga previa muestra el mensaje sin la parte de mostrando`() {
        val texto = textoAviso(
            EstadoDatos(enLinea = false, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 0)
        )
        assertEquals("Sin conexión", texto)
    }

    @Test
    fun `sin conexion con descarga previa y pendientes muestra ambas partes`() {
        val texto = textoAviso(
            EstadoDatos(
                enLinea = false,
                ultimaDescarga = Instant.parse("2026-10-11T15:30:00Z"),
                pendientesPorEnviar = 3,
                necesitanAtencion = 0
            )
        )
        assertTrue(texto!!.startsWith("Sin conexión"))
        assertTrue(texto.contains("mostrando lo descargado a las"))
        assertTrue(texto.contains("3 cambios por enviar"))
    }

    @Test
    fun `con 1 cambio rechazado el verbo va en singular`() {
        val texto = textoAviso(
            EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 1)
        )
        assertEquals("1 cambio no se pudo enviar", texto)
    }

    @Test
    fun `con 2 cambios rechazados el verbo va en plural`() {
        val texto = textoAviso(
            EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 2)
        )
        assertEquals("2 cambios no se pudieron enviar", texto)
    }

    @Test
    fun `entendido se ofrece con conexion y algo rechazado`() {
        // RF-80
        assertTrue(
            puedeDescartar(EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 1, necesitanAtencion = 1))
        )
    }

    @Test
    fun `entendido no se ofrece sin conexion aunque haya algo rechazado`() {
        // El texto habla de la conexión: el botón no le correspondería (D-11 de la spec 005)
        assertFalse(
            puedeDescartar(EstadoDatos(enLinea = false, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 1))
        )
    }

    @Test
    fun `entendido no se ofrece si no hay nada rechazado`() {
        assertFalse(
            puedeDescartar(EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 2, necesitanAtencion = 0))
        )
    }

    @Test
    fun `en linea con pendientes y nada que necesite atencion muestra enviando`() {
        val texto = textoAviso(
            EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 4, necesitanAtencion = 0)
        )
        assertEquals("Enviando 4 cambios…", texto)
    }

    @Test
    fun `en linea sin pendientes y sin nada que necesite atencion no muestra nada`() {
        val texto = textoAviso(
            EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 0)
        )
        assertNull(texto)
    }
}
