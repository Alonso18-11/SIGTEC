package com.softcorp.sigtec.feature.indicadores.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** HU-18 (RF-1813, RF-1816, RF-1817, RF-1820): los textos de los avisos del jefe. */
class AvisosJefeTest {

    @Test
    fun `sin casos demorados no hay aviso`() {
        assertNull(contenidoCasosDemorados(0))
    }

    @Test
    fun `un caso demorado va en singular`() {
        val aviso = contenidoCasosDemorados(1)!!
        assertEquals("Casos demorados", aviso.titulo)
        assertEquals("1 incidencia lleva más de 5 días pendiente.", aviso.texto)
    }

    @Test
    fun `varios casos demorados van en plural y el texto publico no lleva el numero`() {
        val aviso = contenidoCasosDemorados(3)!!
        assertEquals("3 incidencias llevan más de 5 días pendientes.", aviso.texto)
        assertEquals("Casos demorados", aviso.textoPublico)
    }

    @Test
    fun `resumen sin reparados ni pendientes`() {
        val aviso = contenidoResumenDia(reparados = 0, pendientes = 0, criticos = 0)
        assertEquals("Resumen del día", aviso.titulo)
        assertEquals("Hoy no se reparó ningún equipo. No quedan pendientes.", aviso.texto)
        assertEquals("Resumen del día", aviso.textoPublico)
    }

    @Test
    fun `resumen en singular`() {
        assertEquals(
            "Hoy se reparó 1 equipo. Queda 1 pendiente, 1 crítico.",
            contenidoResumenDia(reparados = 1, pendientes = 1, criticos = 1).texto
        )
    }

    @Test
    fun `resumen en plural`() {
        assertEquals(
            "Hoy se repararon 4 equipos. Quedan 7 pendientes, 0 críticos.",
            contenidoResumenDia(reparados = 4, pendientes = 7, criticos = 0).texto
        )
    }

    @Test
    fun `los reparados de hoy son la ultima columna del grafico`() {
        val hoy = LocalDate.of(2026, 11, 16)
        val indicadores = Indicadores(
            reparados7Dias = 5, pendientes = 2, criticos = 1,
            reparadosPorDia = listOf(
                ReparadosDia(hoy.minusDays(1), cantidad = 3, esHoy = false),
                ReparadosDia(hoy, cantidad = 2, esHoy = true)
            ),
            horasPromedioAtencion = null, porEstado = emptyList(), cargas = emptyList(), equipoAEvaluar = null
        )
        assertEquals(2, indicadores.reparadosHoy)
    }
}
