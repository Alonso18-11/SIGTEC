package com.softcorp.sigtec.core.notificaciones

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

/** HU-07 (RF-702, RF-704, RF-707): texto e identificador del aviso de asignación. */
class ContenidoAvisoTest {

    private val incidencia = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "p1", registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.ASIGNADA,
        tecnicoAsignadoId = "u-luis", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    @Test
    fun `el aviso dice codigo, equipo y descripcion`() {
        val contenido = contenidoAvisoAsignacion(incidencia)
        assertEquals("Nueva incidencia asignada", contenido.titulo)
        assertEquals("INC-0127 · IMP-RRHH-002 · La impresora no aparece en la red", contenido.texto)
    }

    @Test
    fun `con el celular bloqueado solo se ve el titulo`() {
        // RF-707
        val publico = contenidoAvisoAsignacion(incidencia).textoPublico
        assertEquals("Nueva incidencia asignada", publico)
        assertFalse(publico.contains("IMP-RRHH-002") || publico.contains("impresora"))
    }

    @Test
    fun `sin numero dice incidencia en lugar del codigo`() {
        assertEquals(
            "Incidencia · IMP-RRHH-002 · La impresora no aparece en la red",
            contenidoAvisoAsignacion(incidencia.copy(numero = null)).texto
        )
    }

    @Test
    fun `la misma incidencia siempre usa el mismo id de notificacion`() {
        // RF-704
        assertEquals(idNotificacion("i127"), idNotificacion("i127"))
        assertNotEquals(idNotificacion("i127"), idNotificacion("i128"))
    }
}
