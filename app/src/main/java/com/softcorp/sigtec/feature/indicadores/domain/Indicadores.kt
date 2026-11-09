package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.feature.asignacion.domain.cargaEfectiva
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val DIAS_GRAFICO = 7
private const val DIAS_CRITICA = 5L
private const val DIAS_PROMEDIO = 30L
private const val DIAS_EQUIPO = 90L
private const val MINIMO_EQUIPO = 3

/** Cierres de un día del gráfico (RF-1509). */
data class ReparadosDia(val dia: LocalDate, val cantidad: Int, val esHoy: Boolean)

/** Una fila de «Incidencias por estado» (RF-1515): las 4 marcas pendientes y CERRADA (Solucionadas). */
data class ConteoEstado(val marca: MarcaSeguimiento, val cantidad: Int)

/** Una fila de «Carga por técnico» (RF-1517 a RF-1519), ya con las asignaciones sin subir. */
data class CargaTecnico(val id: String, val nombre: String, val activas: Int, val limite: Int) {
    val alcanzoLimite: Boolean get() = activas >= limite
}

/** RF-1521: el equipo con más incidencias en 90 días, si llega a 3. */
data class EquipoAEvaluar(val codigo: String, val oficina: String?, val incidencias: Int)

/** Todo lo que muestra el tablero (HU-15). */
data class Indicadores(
    val reparados7Dias: Int,
    val pendientes: Int,
    val criticos: Int,
    val reparadosPorDia: List<ReparadosDia>,      // de hace 6 días a hoy
    val horasPromedioAtencion: Double?,           // null sin cierres en 30 días (RF-1514)
    val porEstado: List<ConteoEstado>,
    val cargas: List<CargaTecnico>,
    val equipoAEvaluar: EquipoAEvaluar?
)

private val ORDEN_ESTADOS = listOf(
    MarcaSeguimiento.SIN_ASIGNAR, MarcaSeguimiento.ASIGNADA, MarcaSeguimiento.EN_ATENCION,
    MarcaSeguimiento.ESPERANDO_REPUESTO, MarcaSeguimiento.CERRADA
)

/**
 * HU-15: los indicadores del área a partir de lo que hay en Room. Función pura para probarla sin Room ni reloj real.
 * [sinSubir] son las asignaciones de este celular aún sin subir, por técnico (RF-1518).
 */
fun calcularIndicadores(
    incidencias: List<Incidencia>,
    cierres: List<Movimiento>,
    tecnicos: List<Tecnico>,
    sinSubir: Map<String, Int>,
    equipos: List<Equipo>,
    ahora: Instant,
    zona: ZoneId
): Indicadores {
    val porId = incidencias.associateBy { it.id }
    // RF-1511: un solo cierre por incidencia, el más reciente, y solo de incidencias que están en el celular
    val cierrePorIncidencia: Map<String, Instant> = cierres
        .filter { it.tipo == TipoMovimiento.CERRADA && it.incidenciaId in porId }
        .groupBy { it.incidenciaId }
        .mapValues { (_, lista) -> lista.maxOf { it.fecha } }

    // RF-1506, RF-1509: los últimos 7 días contando hoy, en el día local
    val hoy = ahora.atZone(zona).toLocalDate()
    val dias = (DIAS_GRAFICO - 1 downTo 0).map { hoy.minusDays(it.toLong()) }
    val cierresPorDia = cierrePorIncidencia.values.groupingBy { it.atZone(zona).toLocalDate() }.eachCount()
    val reparadosPorDia = dias.map { ReparadosDia(it, cierresPorDia[it] ?: 0, esHoy = it == hoy) }

    // RF-1507, RF-1508 (D-1): pendientes, y críticas si llevan más de 5 días o las marcó la HU-18
    val pendientes = incidencias.filter { it.estado == EstadoIncidencia.PENDIENTE }
    val limiteCritica = ahora.minus(Duration.ofDays(DIAS_CRITICA))
    val criticos = pendientes.count { it.critica || it.fechaRegistro.isBefore(limiteCritica) }

    // RF-1514: promedio de registro a cierre, de los cierres de los últimos 30 días
    val desde30 = ahora.minus(Duration.ofDays(DIAS_PROMEDIO))
    val duraciones = cierrePorIncidencia
        .filterValues { !it.isBefore(desde30) }
        .mapNotNull { (id, cierre) -> porId[id]?.let { Duration.between(it.fechaRegistro, cierre).toMinutes() / 60.0 } }
        .filter { it >= 0 }
    val horasPromedio = duraciones.takeIf { it.isNotEmpty() }?.average()

    // RF-1515, RF-1516: todas las marcas, también las que están en 0
    val conteo = incidencias.groupingBy { it.marca }.eachCount()
    val porEstado = ORDEN_ESTADOS.map { ConteoEstado(it, conteo[it] ?: 0) }

    // RF-1517, RF-1518: la misma carga que ve el jefe al asignar
    val cargas = tecnicos.sortedBy { it.nombre }.map { t ->
        val efectivo = cargaEfectiva(t, sinSubir[t.id] ?: 0)
        CargaTecnico(t.id, t.nombre, efectivo.incidenciasActivas, efectivo.limiteAtencion)
    }

    return Indicadores(
        reparados7Dias = reparadosPorDia.sumOf { it.cantidad },
        pendientes = pendientes.size,
        criticos = criticos,
        reparadosPorDia = reparadosPorDia,
        horasPromedioAtencion = horasPromedio,
        porEstado = porEstado,
        cargas = cargas,
        equipoAEvaluar = equipoAEvaluar(incidencias, equipos, ahora)
    )
}

/** RF-1521, RF-1523: el de más incidencias en 90 días (empate: el de la más reciente), si tiene 3 o más. */
private fun equipoAEvaluar(incidencias: List<Incidencia>, equipos: List<Equipo>, ahora: Instant): EquipoAEvaluar? {
    val desde = ahora.minus(Duration.ofDays(DIAS_EQUIPO))
    val elegido = incidencias
        .filter { !it.fechaRegistro.isBefore(desde) }
        .groupBy { it.codigoEquipo }
        .maxWithOrNull(
            compareBy<Map.Entry<String, List<Incidencia>>>({ it.value.size }, { e -> e.value.maxOf { it.fechaRegistro } })
        )
        ?.takeIf { it.value.size >= MINIMO_EQUIPO }
        ?: return null
    val oficina = equipos.firstOrNull { it.codigo == elegido.key }?.oficina
    return EquipoAEvaluar(elegido.key, oficina, elegido.value.size)
}
