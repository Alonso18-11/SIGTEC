package com.softcorp.sigtec.feature.indicadores.presentation

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.indicadores.domain.AtencionEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** HU-16: los textos del historial del equipo. */
class TextosHistorialTest {

    private val lima = ZoneId.of("America/Lima")
    // 27/09/2026 a las 09:30 en Lima
    private val registro = Instant.parse("2026-09-27T14:30:00Z")

    private fun atencion(
        marca: MarcaSeguimiento = MarcaSeguimiento.CERRADA,
        numero: String? = "INC-0124",
        trabajo: String? = "Se cambió la fuente",
        tecnico: String? = "Luis Ramírez",
        critica: Boolean = false
    ) = AtencionEquipo("i1", numero, registro, "No enciende después del corte de luz", marca, tecnico, trabajo, critica)

    @Test
    fun `el promedio va en dias con coma, en horas si es menos de un dia y con guion si no hay`() {
        assertEquals(TextoMetrica("2,1", "Días promedio en 3 meses"), metricaPromedio(50.4))
        assertEquals(TextoMetrica("1,0", "Días promedio en 3 meses"), metricaPromedio(24.0))
        assertEquals(TextoMetrica("6", "Horas promedio en 3 meses"), metricaPromedio(6.2))
        assertEquals(TextoMetrica("1", "Horas promedio en 3 meses"), metricaPromedio(0.2))
        assertEquals(TextoMetrica("-", "Días promedio en 3 meses"), metricaPromedio(null))
    }

    @Test
    fun `las lineas del equipo usan comas y omiten lo vacio`() {
        val equipo = Equipo("PC-CONT-014", "Computadora de escritorio", "HP", "ProDesk 400", "Contabilidad", "María Quispe")
        assertEquals(
            listOf("Computadora de escritorio, HP ProDesk 400", "Contabilidad, responsable María Quispe"),
            lineasEquipo(equipo)
        )
        assertEquals(listOf("Laptop", "Logística"), lineasEquipo(equipo.copy(tipo = "Laptop", marca = "", modelo = "", oficina = "Logística", responsableNombre = "")))
    }

    @Test
    fun `el encabezado lleva la fecha y el codigo o por sincronizar`() {
        assertEquals("27/09/2026, INC-0124", encabezadoAtencion(atencion(), lima))
        assertEquals("27/09/2026, Por sincronizar", encabezadoAtencion(atencion(numero = null), lima))
    }

    @Test
    fun `el trabajo solo se muestra en las cerradas y avisa si el informe no llego`() {
        assertEquals("Se cambió la fuente", trabajoAtencion(atencion()))
        assertEquals("Informe por descargar", trabajoAtencion(atencion(trabajo = null)))
        assertNull(trabajoAtencion(atencion(MarcaSeguimiento.EN_ATENCION, trabajo = null)))
    }

    @Test
    fun `sin tecnico lo dice`() {
        assertEquals("Luis Ramírez", tecnicoAtencion(atencion()))
        assertEquals("Sin técnico", tecnicoAtencion(atencion(MarcaSeguimiento.SIN_ASIGNAR, tecnico = null)))
    }

    @Test
    fun `talkback lee la atencion como una sola frase con la fecha completa`() {
        assertEquals(
            "27 de septiembre de 2026, INC-0124, En atención, No enciende después del corte de luz, Luis Ramírez",
            descripcionAtencion(atencion(MarcaSeguimiento.EN_ATENCION, trabajo = null), lima)
        )
        assertEquals(
            "27 de septiembre de 2026, INC-0124, Solucionado, No enciende después del corte de luz, Se cambió la fuente, Luis Ramírez",
            descripcionAtencion(atencion(), lima)
        )
        assertEquals(
            "27 de septiembre de 2026, Por sincronizar, Crítica, Sin asignar, No enciende después del corte de luz, Sin técnico",
            descripcionAtencion(atencion(MarcaSeguimiento.SIN_ASIGNAR, numero = null, trabajo = null, tecnico = null, critica = true), lima)
        )
    }

    @Test
    fun `los dias desde el registro nunca son negativos`() {
        assertEquals(6, diasDesdeRegistro(atencion(), registro.plusSeconds(6 * 86_400 + 60)))
        assertEquals(0, diasDesdeRegistro(atencion(), registro.minusSeconds(60)))
    }
}
