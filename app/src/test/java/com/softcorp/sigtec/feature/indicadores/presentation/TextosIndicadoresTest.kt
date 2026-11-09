package com.softcorp.sigtec.feature.indicadores.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** HU-15 (RF-1502, RF-1509, RF-1512, RF-1514, RF-1521): los textos del tablero. */
class TextosIndicadoresTest {

    private val lima = ZoneId.of("America/Lima")
    private val ahora = Instant.parse("2026-11-09T20:00:00Z")   // lunes 9, 15:00 en Lima

    @Test
    fun `actualizado hoy, ayer, otro dia y sin descargas`() {
        assertEquals("Actualizado hoy a las 14:05", textoActualizado(Instant.parse("2026-11-09T19:05:00Z"), ahora, lima))
        assertEquals("Actualizado ayer a las 09:10", textoActualizado(Instant.parse("2026-11-08T14:10:00Z"), ahora, lima))
        assertEquals("Actualizado el 07/11 a las 08:00", textoActualizado(Instant.parse("2026-11-07T13:00:00Z"), ahora, lima))
        assertEquals("Sin datos del servidor todavía", textoActualizado(null, ahora, lima))
    }

    @Test
    fun `tiempo de atencion en dias con coma o en horas`() {
        assertEquals("1,8 días de atención", textoTiempoAtencion(43.2))
        assertEquals("6 h de atención", textoTiempoAtencion(6.2))
        assertEquals("1 h de atención", textoTiempoAtencion(0.2))
        assertEquals("1,0 días de atención", textoTiempoAtencion(24.0))
    }

    @Test
    fun `dias de la semana cortos y largos`() {
        assertEquals("Lun", diaCorto(LocalDate.of(2026, 11, 9)))
        assertEquals("Mié", diaCorto(LocalDate.of(2026, 11, 11)))
        assertEquals("Dom", diaCorto(LocalDate.of(2026, 11, 8)))
        assertEquals("sábado", diaLargo(LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun `equipo a evaluar`() {
        assertEquals("4 incidencias en 3 meses, evaluar reemplazo", textoEquipoAEvaluar(4))
    }
}
