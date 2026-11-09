package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** HU-15: el cálculo del tablero, sin Room ni reloj real. */
class CalcularIndicadoresTest {

    private val lima = ZoneId.of("America/Lima")
    // Lunes 9 de noviembre de 2026, 15:00 en Lima
    private val ahora = Instant.parse("2026-11-09T20:00:00Z")

    private fun hace(dias: Long, horas: Long = 0) = ahora.minus(Duration.ofDays(dias)).minus(Duration.ofHours(horas))

    private fun incidencia(
        id: String, registro: Instant, marca: MarcaSeguimiento = MarcaSeguimiento.ASIGNADA,
        equipo: String = "PC-CONT-014", critica: Boolean = false
    ) = Incidencia(
        id = id, numero = 1, codigoEquipo = equipo, descripcion = "No enciende", usuarioResponsable = "María Quispe",
        fechaRegistro = registro, registradoPorId = "u", registradoPorNombre = "Carlos Mendoza", marca = marca,
        critica = critica
    )

    private fun cierre(incidenciaId: String, fecha: Instant, id: String = "m-$incidenciaId-$fecha") =
        Movimiento(id, incidenciaId, TipoMovimiento.CERRADA, "Luis Ramírez", fecha)

    private fun calcular(
        incidencias: List<Incidencia> = emptyList(),
        cierres: List<Movimiento> = emptyList(),
        tecnicos: List<Tecnico> = emptyList(),
        sinSubir: Map<String, Int> = emptyMap(),
        equipos: List<Equipo> = emptyList()
    ) = calcularIndicadores(incidencias, cierres, tecnicos, sinSubir, equipos, ahora, lima)

    @Test
    fun `sin datos todo en cero y siete dias con hoy al final`() {
        val r = calcular()
        assertEquals(0, r.reparados7Dias)
        assertEquals(0, r.pendientes)
        assertEquals(0, r.criticos)
        assertNull(r.horasPromedioAtencion)
        assertNull(r.equipoAEvaluar)
        assertEquals(7, r.reparadosPorDia.size)
        assertEquals(LocalDate.of(2026, 11, 3), r.reparadosPorDia.first().dia)
        assertEquals(LocalDate.of(2026, 11, 9), r.reparadosPorDia.last().dia)
        assertTrue(r.reparadosPorDia.last().esHoy)
        assertEquals(5, r.porEstado.size)
        assertTrue(r.porEstado.all { it.cantidad == 0 })
    }

    @Test
    fun `los cierres cuentan en su dia local y solo los de los ultimos siete dias`() {
        val inc = (1..4).map { incidencia("i$it", hace(10), MarcaSeguimiento.CERRADA) }
        val r = calcular(
            incidencias = inc,
            cierres = listOf(
                cierre("i1", hace(0, 2)),                       // hoy
                cierre("i2", Instant.parse("2026-11-09T04:30:00Z")),  // 23:30 del domingo en Lima
                cierre("i3", hace(3)),
                cierre("i4", hace(8))                           // fuera de los 7 días
            )
        )
        assertEquals(3, r.reparados7Dias)
        assertEquals(1, r.reparadosPorDia.last().cantidad)
        assertEquals(1, r.reparadosPorDia.first { it.dia == LocalDate.of(2026, 11, 8) }.cantidad)
        assertEquals(1, r.reparadosPorDia.first { it.dia == LocalDate.of(2026, 11, 6) }.cantidad)
    }

    @Test
    fun `una incidencia con dos cierres cuenta una vez con el mas reciente`() {
        val r = calcular(
            incidencias = listOf(incidencia("i1", hace(4), MarcaSeguimiento.CERRADA)),
            cierres = listOf(cierre("i1", hace(9)), cierre("i1", hace(1)))
        )
        assertEquals(1, r.reparados7Dias)
    }

    @Test
    fun `un cierre de una incidencia que no esta en el celular no cuenta`() {
        assertEquals(0, calcular(cierres = listOf(cierre("otra", hace(1)))).reparados7Dias)
    }

    @Test
    fun `pendientes y criticos por mas de cinco dias o por el campo critica`() {
        val r = calcular(
            incidencias = listOf(
                incidencia("a", hace(1)),                                     // pendiente, no crítica
                incidencia("b", hace(6), MarcaSeguimiento.EN_ATENCION),       // crítica por días
                incidencia("c", hace(2), MarcaSeguimiento.SIN_ASIGNAR, critica = true),  // crítica por la HU-18
                incidencia("d", hace(20), MarcaSeguimiento.CERRADA)           // solucionada: no cuenta
            )
        )
        assertEquals(3, r.pendientes)
        assertEquals(2, r.criticos)
    }

    @Test
    fun `tiempo promedio de registro a cierre de los ultimos treinta dias`() {
        val r = calcular(
            incidencias = listOf(
                incidencia("a", hace(3), MarcaSeguimiento.CERRADA),
                incidencia("b", hace(5), MarcaSeguimiento.CERRADA),
                incidencia("c", hace(60), MarcaSeguimiento.CERRADA)
            ),
            cierres = listOf(
                cierre("a", hace(2)),     // 24 h
                cierre("b", hace(2)),     // 72 h
                cierre("c", hace(40))     // fuera de los 30 días
            )
        )
        assertEquals(48.0, r.horasPromedioAtencion!!, 0.01)
    }

    @Test
    fun `estados en orden con las solucionadas al final y las marcas en cero`() {
        val r = calcular(
            incidencias = listOf(
                incidencia("a", hace(1), MarcaSeguimiento.SIN_ASIGNAR),
                incidencia("b", hace(1), MarcaSeguimiento.SIN_ASIGNAR),
                incidencia("c", hace(1), MarcaSeguimiento.CERRADA)
            )
        )
        assertEquals(
            listOf(
                MarcaSeguimiento.SIN_ASIGNAR to 2, MarcaSeguimiento.ASIGNADA to 0, MarcaSeguimiento.EN_ATENCION to 0,
                MarcaSeguimiento.ESPERANDO_REPUESTO to 0, MarcaSeguimiento.CERRADA to 1
            ),
            r.porEstado.map { it.marca to it.cantidad }
        )
    }

    @Test
    fun `carga por nombre con las asignaciones sin subir y el limite alcanzado`() {
        val r = calcular(
            tecnicos = listOf(
                Tecnico("u-rosa", "Rosa Vilca", 4, 2),
                Tecnico("u-ana", "Ana Torres", 5, 4),
                Tecnico("u-cero", "Zoe Mal Configurada", 0, 0)
            ),
            sinSubir = mapOf("u-ana" to 1)
        )
        assertEquals(listOf("Ana Torres", "Rosa Vilca", "Zoe Mal Configurada"), r.cargas.map { it.nombre })
        assertEquals(5, r.cargas[0].activas)
        assertTrue(r.cargas[0].alcanzoLimite)
        assertFalse(r.cargas[1].alcanzoLimite)
        assertTrue(r.cargas[2].alcanzoLimite)   // límite 0: sin dividir por cero
    }

    @Test
    fun `equipo a evaluar con tres o mas incidencias en noventa dias y su oficina`() {
        val equipos = listOf(Equipo("PC-CONT-014", "PC", "HP", "ProDesk", "Contabilidad", "María Quispe"))
        val tres = (1..3).map { incidencia("c$it", hace(it * 10L)) }
        val viejas = (1..5).map { incidencia("v$it", hace(100L + it), equipo = "IMP-RRHH-002") }
        val r = calcular(incidencias = tres + viejas, equipos = equipos)
        assertEquals(EquipoAEvaluar("PC-CONT-014", "Contabilidad", 3), r.equipoAEvaluar)

        assertNull(calcular(incidencias = tres.take(2) + viejas).equipoAEvaluar)
    }

    @Test
    fun `con empate en el equipo gana el de la incidencia mas reciente`() {
        val a = (1..3).map { incidencia("a$it", hace(20L + it), equipo = "PC-A") }
        val b = (1..3).map { incidencia("b$it", hace(it.toLong()), equipo = "PC-B") }
        assertEquals("PC-B", calcular(incidencias = a + b).equipoAEvaluar?.codigo)
        assertNull(calcular(incidencias = a + b).equipoAEvaluar?.oficina)
    }

    @Test
    fun `con dos mil incidencias calcula en menos de un segundo`() {
        val muchas = (1..2000).map {
            incidencia("i$it", hace(it % 120L), if (it % 3 == 0) MarcaSeguimiento.CERRADA else MarcaSeguimiento.ASIGNADA,
                equipo = "EQ-${it % 50}")
        }
        val cierres = muchas.filter { it.marca == MarcaSeguimiento.CERRADA }.map { cierre(it.id, it.fechaRegistro.plusSeconds(3600)) }
        val inicio = System.nanoTime()
        calcular(incidencias = muchas, cierres = cierres)
        assertTrue((System.nanoTime() - inicio) / 1e9 < 1)
    }
}
