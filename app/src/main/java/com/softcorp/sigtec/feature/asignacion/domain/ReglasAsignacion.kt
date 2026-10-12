package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoMovimiento

/**
 * Un técnico tal como lo muestra la pantalla de asignación (RF-601 a RF-604): `tecnico` ya trae
 * la carga efectiva de `cargaEfectiva`, así que `disponible` y `cargaRelativa` salen de ella.
 */
data class OpcionTecnico(
    val tecnico: Tecnico,
    val disponible: Boolean,
    val esActual: Boolean          // RF-604: el que ya la tiene asignada no se vuelve a elegir
)

/**
 * RF-602: la carga que cuenta el servidor más las asignaciones a ese técnico que este celular
 * guardó y todavía no subió. Se reutilizan `disponible` y `cargaRelativa` del modelo sin tocarlo.
 */
fun cargaEfectiva(tecnico: Tecnico, sinSubir: Int): Tecnico =
    tecnico.copy(incidenciasActivas = tecnico.incidenciasActivas + sinSubir)

/**
 * RF-609, RF-610 (y RF-607 en pantalla): por qué no se puede asignar, o null si se puede. Una sola
 * regla para el caso de uso y la pantalla. Sin número no se asigna porque el servidor solo acepta
 * crear incidencias sin asignar (D-3 del plan).
 */
fun bloqueoParaAsignar(incidencia: Incidencia): ErrorDominio? = when {
    incidencia.estado == EstadoIncidencia.SOLUCIONADO -> ErrorDominio.IncidenciaCerrada
    incidencia.numero == null -> ErrorDominio.IncidenciaPorSincronizar
    else -> null
}

/**
 * RF-614, RF-625: el técnico nuevo todavía no inicia la atención, así que queda Asignada; pero si
 * esperaba un repuesto, conserva esa marca porque la pieza sigue en camino.
 */
fun marcaAlAsignar(actual: MarcaSeguimiento): MarcaSeguimiento =
    if (actual == MarcaSeguimiento.ESPERANDO_REPUESTO) actual else MarcaSeguimiento.ASIGNADA

/** RF-613: ASIGNADA si no tenía técnico; REASIGNADA si tenía otro. */
fun tipoDeAsignacion(tecnicoAnteriorId: String?): TipoMovimiento =
    if (tecnicoAnteriorId == null) TipoMovimiento.ASIGNADA else TipoMovimiento.REASIGNADA

/** RF-615: lo que el detalle muestra bajo el título del movimiento (D-4 del plan). */
fun detalleDeAsignacion(anterior: String?, nuevo: String, jefe: String): String =
    if (anterior == null) "A $nuevo, por $jefe" else "De $anterior a $nuevo, por $jefe"

/** RF-601 a RF-604: los técnicos por nombre, con su carga efectiva, disponibilidad y si es el actual. */
fun armarOpciones(
    incidencia: Incidencia,
    tecnicos: List<Tecnico>,
    sinSubir: Map<String, Int>
): List<OpcionTecnico> =
    tecnicos.sortedBy { it.nombre }.map { tecnico ->
        val efectivo = cargaEfectiva(tecnico, sinSubir[tecnico.id] ?: 0)
        OpcionTecnico(
            tecnico = efectivo,
            disponible = efectivo.disponible,
            esActual = tecnico.id == incidencia.tecnicoAsignadoId
        )
    }
