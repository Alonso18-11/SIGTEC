package com.softcorp.sigtec.feature.ia.data

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-14 (RF-1401, RF-1417, RNF de privacidad): a Gemini solo va el texto del problema. */
class InstruccionesGeminiTest {

    private val incidencia = Incidencia(
        id = "inc-1", numero = 124, codigoEquipo = "PC-CONT-014",
        descripcion = "  El equipo no enciende desde el corte de luz.  ",
        usuarioResponsable = "María Quispe Huamán", areaResponsable = "Contabilidad",
        fechaRegistro = Instant.parse("2026-11-15T14:00:00Z"),
        registradoPorId = "u-luis", registradoPorNombre = "Luis Ramírez",
        marca = MarcaSeguimiento.EN_ATENCION,
        tecnicoAsignadoId = "u-luis", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private val datosPersonales = listOf("María", "Quispe", "Contabilidad", "Luis", "Ramírez", "PC-CONT-014", "INC-0124")

    @Test
    fun `la clasificacion lleva la descripcion sin espacios de mas`() {
        assertEquals(
            "Descripción del problema:\nEl equipo no enciende desde el corte de luz.",
            pedidoClasificacion(incidencia.descripcion)
        )
    }

    @Test
    fun `la clasificacion no lleva datos personales ni el codigo del equipo`() {
        val pedido = pedidoClasificacion(incidencia.descripcion)
        datosPersonales.forEach { assertFalse("no debe llevar «$it»", pedido.contains(it)) }
    }

    @Test
    fun `el borrador lleva la descripcion, las notas y solo los campos escritos`() {
        val pedido = pedidoBorrador(
            descripcion = incidencia.descripcion,
            notas = "cambie la fuente, ahora prende",
            diagnostico = "Fuente dañada",
            piezasCambiadas = "   "
        )
        assertTrue(pedido.contains("Notas del técnico:\ncambie la fuente, ahora prende"))
        assertTrue(pedido.contains("«Diagnóstico»:\nFuente dañada"))
        assertFalse(pedido.contains("Piezas cambiadas"))
        assertFalse(pedido.contains("Trabajo realizado"))
        datosPersonales.forEach { assertFalse("no debe llevar «$it»", pedido.contains(it)) }
    }

    @Test
    fun `cada texto se recorta al maximo`() {
        val pedido = pedidoClasificacion("x".repeat(MAX_CARACTERES_PEDIDO + 100))
        assertEquals("Descripción del problema:\n".length + MAX_CARACTERES_PEDIDO, pedido.length)
    }

    @Test
    fun `las instrucciones nombran los valores que la app sabe leer`() {
        listOf("HARDWARE", "SOFTWARE", "RED", "ALTA", "MEDIA", "BAJA").forEach {
            assertTrue(INSTRUCCIONES_CLASIFICACION.contains(it))
        }
        listOf("diagnostico", "trabajoRealizado", "piezasCambiadas", "observaciones").forEach {
            assertTrue(INSTRUCCIONES_BORRADOR.contains(it))
        }
    }
}
