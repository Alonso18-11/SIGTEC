package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Equipo
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.feature.incidencias.domain.esCritica
import java.time.Duration
import java.time.Instant

private const val MINUTOS_HORA = 60.0

/**
 * Una atención del equipo (RF-1616 a RF-1618): la incidencia con lo que dice su informe. [trabajoRealizado] es null si
 * está pendiente o si su informe aún no llegó al celular ([informeDescargado] dice cuál de los dos).
 */
data class AtencionEquipo(
    val incidenciaId: String,
    val codigoVisible: String?,          // null hasta que la sincronización le da número («Por sincronizar»)
    val fechaRegistro: Instant,
    val descripcion: String,
    val marca: MarcaSeguimiento,
    val tecnicoNombre: String?,          // el asignado; se conserva al cerrar
    val trabajoRealizado: String?,
    val critica: Boolean = false         // la misma regla que la lista (HU-15, D-1), para su `EtiquetaIncidencia`
) {
    val cerrada: Boolean get() = marca == MarcaSeguimiento.CERRADA
    val informeDescargado: Boolean get() = !cerrada || trabajoRealizado != null
}

/** Todo lo que muestra «Historial del equipo» (HU-16). Las métricas son de los últimos 90 días (D-3). */
data class HistorialEquipo(
    val codigo: String,
    val equipo: Equipo?,                 // null si el código no está en el catálogo (RF-1608)
    val atencionesTresMeses: Int,        // RF-1610
    val piezasTresMeses: Int,            // RF-1611
    val horasPromedioTresMeses: Double?, // RF-1612; null sin ninguna cerrada con su cierre
    val critico: Boolean,                // RF-1613 (D-2)
    val atenciones: List<AtencionEquipo> // RF-1615, de la más reciente a la más antigua (D-4)
)

/** RF-1604: el código escrito o escaneado se busca sin espacios en los extremos y en mayúsculas, como RF-34. */
fun normalizarCodigoEquipo(texto: String): String = texto.trim().uppercase()

/**
 * HU-16: el historial de un equipo a partir de lo que hay en Room. Función pura para probarla sin Room ni reloj real.
 * Recibe las incidencias, cierres e informes de ese equipo; los de otros equipos se ignoran.
 */
fun calcularHistorial(
    codigo: String,
    equipo: Equipo?,
    incidencias: List<Incidencia>,
    cierres: List<Movimiento>,
    informes: List<InformeTecnico>,
    ahora: Instant
): HistorialEquipo {
    val delEquipo = incidencias.filter { it.codigoEquipo == codigo }
    val informePorIncidencia = informes.associateBy { it.incidenciaId }
    // Como el tablero (RF-1511): un solo cierre por incidencia, el más reciente
    val cierrePorIncidencia: Map<String, Instant> = cierres
        .filter { it.tipo == TipoMovimiento.CERRADA }
        .groupBy { it.incidenciaId }
        .mapValues { (_, lista) -> lista.maxOf { it.fecha } }

    // RF-1615 (D-4): de la más reciente a la más antigua
    val atenciones = delEquipo.sortedByDescending { it.fechaRegistro }.map { inc ->
        AtencionEquipo(
            incidenciaId = inc.id,
            codigoVisible = inc.codigoVisible,
            fechaRegistro = inc.fechaRegistro,
            descripcion = inc.descripcion,
            marca = inc.marca,
            tecnicoNombre = inc.tecnicoAsignadoNombre,
            // RF-1617: solo las cerradas muestran el trabajo de su informe
            trabajoRealizado = if (inc.estado == EstadoIncidencia.SOLUCIONADO) {
                informePorIncidencia[inc.id]?.trabajoRealizado
            } else {
                null
            },
            critica = esCritica(inc, ahora)
        )
    }

    // RF-1609 (D-3): las tres métricas miran las mismas incidencias, las registradas en los últimos 90 días
    val recientes = delEquipo.filter { registradaEnTresMeses(it, ahora) }
    val cerradasRecientes = recientes.filter { it.estado == EstadoIncidencia.SOLUCIONADO }
    // RF-1611: una por atención con «Piezas cambiadas» escrito, no por pieza
    val piezas = cerradasRecientes.count { informePorIncidencia[it.id]?.piezasCambiadas?.isNotBlank() == true }
    // RF-1612: registro a cierre, como el tablero (RF-1514); sin su movimiento CERRADA no cuenta todavía
    val duraciones = cerradasRecientes
        .mapNotNull { inc -> cierrePorIncidencia[inc.id]?.let { Duration.between(inc.fechaRegistro, it) } }
        .filter { !it.isNegative }
        .map { it.toMinutes() / MINUTOS_HORA }

    return HistorialEquipo(
        codigo = codigo,
        equipo = equipo,
        atencionesTresMeses = recientes.size,
        piezasTresMeses = piezas,
        horasPromedioTresMeses = duraciones.takeIf { it.isNotEmpty() }?.average(),
        critico = esEquipoCritico(recientes.size),
        atenciones = atenciones
    )
}
