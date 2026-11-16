package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** HU-16: el cálculo del historial de un equipo, sin Room ni reloj real. */
class CalcularHistorialTest {

    private val ahora = Instant.parse("2026-11-16T15:00:00Z")
    private val codigo = "PC-CONT-014"
    private val equipo = Equipo(codigo, "Computadora de escritorio", "HP", "ProDesk 400", "Contabilidad", "María Quispe")

    private fun hace(dias: Long, horas: Long = 0) = ahora.minus(Duration.ofDays(dias)).minus(Duration.ofHours(horas))

    private fun incidencia(
        id: String,
        registro: Instant,
        marca: MarcaSeguimiento = MarcaSeguimiento.CERRADA,
        equipo: String = codigo,
        numero: Int? = 1,
        tecnico: String? = "Luis Ramírez"
    ) = Incidencia(
        id = id, numero = numero, codigoEquipo = equipo, descripcion = "Falla $id", usuarioResponsable = "María Quispe",
        fechaRegistro = registro, registradoPorId = "u", registradoPorNombre = "Carlos Mendoza", marca = marca,
        tecnicoAsignadoId = tecnico?.let { "t1" }, tecnicoAsignadoNombre = tecnico
    )

    private fun cierre(incidenciaId: String, fecha: Instant, id: String = "m-$incidenciaId-$fecha") =
        Movimiento(id, incidenciaId, TipoMovimiento.CERRADA, "Luis Ramírez", fecha)

    private fun informe(incidenciaId: String, piezas: String? = null, trabajo: String = "Trabajo $incidenciaId") =
        InformeTecnico(
            id = "inf-$incidenciaId", incidenciaId = incidenciaId, diagnostico = "Diagnóstico",
            trabajoRealizado = trabajo, piezasCambiadas = piezas, observaciones = null,
            fechaCierre = ahora, confirmadoEn = ahora
        )

    private fun calcular(
        incidencias: List<Incidencia> = emptyList(),
        cierres: List<Movimiento> = emptyList(),
        informes: List<InformeTecnico> = emptyList(),
        equipoCatalogo: Equipo? = equipo
    ) = calcularHistorial(codigo, equipoCatalogo, incidencias, cierres, informes, ahora)

    @Test
    fun `sin incidencias las metricas quedan en cero y sin promedio`() {
        val h = calcular()
        assertEquals(codigo, h.codigo)
        assertEquals(equipo, h.equipo)
        assertEquals(0, h.atencionesTresMeses)
        assertEquals(0, h.piezasTresMeses)
        assertNull(h.horasPromedioTresMeses)
        assertFalse(h.critico)
        assertTrue(h.atenciones.isEmpty())
    }

    @Test
    fun `fuera del catalogo el equipo es null y el historial se calcula igual`() {
        val h = calcular(incidencias = listOf(incidencia("a", hace(1))), equipoCatalogo = null)
        assertNull(h.equipo)
        assertEquals(1, h.atenciones.size)
    }

    @Test
    fun `las atenciones van de la mas reciente a la mas antigua y solo las del equipo`() {
        val h = calcular(
            incidencias = listOf(
                incidencia("vieja", hace(200)),
                incidencia("nueva", hace(1)),
                incidencia("otra", hace(0), equipo = "PC-LOG-009"),
                incidencia("media", hace(30))
            )
        )
        assertEquals(listOf("nueva", "media", "vieja"), h.atenciones.map { it.incidenciaId })
    }

    @Test
    fun `una cerrada muestra el trabajo de su informe y una pendiente solo su tecnico`() {
        val h = calcular(
            incidencias = listOf(
                incidencia("cerrada", hace(5)),
                incidencia("pendiente", hace(2), MarcaSeguimiento.EN_ATENCION),
                incidencia("sin-tecnico", hace(1), MarcaSeguimiento.SIN_ASIGNAR, numero = null, tecnico = null)
            ),
            informes = listOf(informe("cerrada", trabajo = "Se cambió el teclado"), informe("pendiente"))
        )
        val porId = h.atenciones.associateBy { it.incidenciaId }
        assertEquals("Se cambió el teclado", porId.getValue("cerrada").trabajoRealizado)
        assertEquals("Luis Ramírez", porId.getValue("cerrada").tecnicoNombre)
        assertNull(porId.getValue("pendiente").trabajoRealizado)
        assertTrue(porId.getValue("pendiente").informeDescargado)
        assertNull(porId.getValue("sin-tecnico").tecnicoNombre)
        assertNull(porId.getValue("sin-tecnico").codigoVisible)
        assertEquals("INC-0001", porId.getValue("cerrada").codigoVisible)
    }

    @Test
    fun `una pendiente de mas de cinco dias es critica como en la lista y una cerrada no`() {
        val h = calcular(
            incidencias = listOf(
                incidencia("demorada", hace(6), MarcaSeguimiento.ASIGNADA),
                incidencia("reciente", hace(2), MarcaSeguimiento.ASIGNADA),
                incidencia("cerrada", hace(30))
            )
        )
        val porId = h.atenciones.associateBy { it.incidenciaId }
        assertTrue(porId.getValue("demorada").critica)
        assertFalse(porId.getValue("reciente").critica)
        assertFalse(porId.getValue("cerrada").critica)
    }

    @Test
    fun `una cerrada cuyo informe no llego queda como informe por descargar`() {
        val h = calcular(incidencias = listOf(incidencia("a", hace(3))))
        assertNull(h.atenciones.single().trabajoRealizado)
        assertFalse(h.atenciones.single().informeDescargado)
    }

    @Test
    fun `las tres metricas miran solo las registradas en los ultimos noventa dias`() {
        val h = calcular(
            incidencias = listOf(
                incidencia("r1", hace(10)),
                incidencia("r2", hace(90)),
                incidencia("p", hace(20), MarcaSeguimiento.ASIGNADA),
                incidencia("vieja", hace(91))            // registrada antes: no cuenta aunque cierre hace poco
            ),
            cierres = listOf(cierre("r1", hace(9)), cierre("r2", hace(88)), cierre("vieja", hace(1))),
            informes = listOf(informe("r1", "RAM 16 GB"), informe("r2", "SSD"), informe("vieja", "Teclado"))
        )
        assertEquals(3, h.atencionesTresMeses)
        assertEquals(2, h.piezasTresMeses)
        assertEquals(36.0, h.horasPromedioTresMeses!!, 0.001)   // 1 día y 2 días
        assertEquals(4, h.atenciones.size)
    }

    @Test
    fun `piezas en blanco o sin informe no cuentan y las pendientes tampoco`() {
        val h = calcular(
            incidencias = listOf(
                incidencia("blanco", hace(1)),
                incidencia("nulo", hace(2)),
                incidencia("sin-informe", hace(3)),
                incidencia("pendiente", hace(4), MarcaSeguimiento.EN_ATENCION),
                incidencia("con", hace(5))
            ),
            informes = listOf(informe("blanco", "   "), informe("nulo", null), informe("pendiente", "Fuente"), informe("con", "Fuente"))
        )
        assertEquals(1, h.piezasTresMeses)
    }

    @Test
    fun `el promedio usa el cierre mas reciente y descarta las cerradas sin cierre`() {
        val h = calcular(
            incidencias = listOf(incidencia("a", hace(3)), incidencia("sin-cierre", hace(2))),
            cierres = listOf(cierre("a", hace(3).plus(Duration.ofHours(2)), "m1"), cierre("a", hace(3).plus(Duration.ofHours(6)), "m2"))
        )
        assertEquals(6.0, h.horasPromedioTresMeses!!, 0.001)
    }

    @Test
    fun `sin cierres de las recientes no hay promedio`() {
        val h = calcular(incidencias = listOf(incidencia("a", hace(3)), incidencia("b", hace(2), MarcaSeguimiento.ASIGNADA)))
        assertNull(h.horasPromedioTresMeses)
    }

    @Test
    fun `es critico con tres registradas en tres meses y no con dos`() {
        val dos = listOf(incidencia("a", hace(1)), incidencia("b", hace(40)), incidencia("vieja", hace(120)))
        assertFalse(calcular(incidencias = dos).critico)
        assertTrue(calcular(incidencias = dos + incidencia("c", hace(80), MarcaSeguimiento.SIN_ASIGNAR)).critico)
    }

    @Test
    fun `normaliza el codigo escrito con espacios y minusculas`() {
        assertEquals("PC-CONT-014", normalizarCodigoEquipo("  pc-cont-014 "))
    }

    @Test
    fun `quinientas incidencias se calculan en menos de un segundo`() {
        val muchas = (1..500).map { incidencia("i$it", hace(it % 200L)) }
        val cierres = muchas.map { cierre(it.id, it.fechaRegistro.plus(Duration.ofHours(30))) }
        val informes = muchas.map { informe(it.id, "Pieza") }
        val inicio = System.nanoTime()
        val h = calcular(muchas, cierres, informes)
        val ms = (System.nanoTime() - inicio) / 1_000_000
        assertEquals(500, h.atenciones.size)
        assertTrue("tardó $ms ms", ms < 1_000)
    }
}
