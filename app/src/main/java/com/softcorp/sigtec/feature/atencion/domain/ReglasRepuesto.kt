package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.Usuario
import java.time.Instant
import java.time.temporal.ChronoUnit

// HU-11 (plan 3.2): las reglas de la solicitud de repuesto, en Kotlin puro para probarlas sin celular.

/** RF-1102 y D-5: las marcas en que el técnico asignado puede pedir una pieza (las mismas del botón del detalle). */
val MARCAS_PARA_SOLICITAR = setOf(
    MarcaSeguimiento.ASIGNADA, MarcaSeguimiento.EN_ATENCION, MarcaSeguimiento.ESPERANDO_REPUESTO
)

/** RF-1102: `null` si [usuario] puede pedir un repuesto para [incidencia]; si no, el error del dominio. */
fun bloqueoParaSolicitar(usuario: Usuario, incidencia: Incidencia): ErrorDominio? = when {
    !usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) -> ErrorDominio.SinPermiso
    incidencia.tecnicoAsignadoId != usuario.id -> ErrorDominio.SinPermiso
    incidencia.marca == MarcaSeguimiento.CERRADA -> ErrorDominio.IncidenciaCerrada
    incidencia.marca !in MARCAS_PARA_SOLICITAR -> ErrorDominio.SinPermiso
    else -> null
}

/** RF-1113: pedir un repuesto deja la incidencia en «Esperando repuesto»; si ya lo estaba, no cambia. */
fun marcaAlSolicitar(actual: MarcaSeguimiento): MarcaSeguimiento =
    if (actual == MarcaSeguimiento.ESPERANDO_REPUESTO) actual else MarcaSeguimiento.ESPERANDO_REPUESTO

/** RF-1105 a RF-1108: lo que el técnico ve de la pieza mientras arma la solicitud. */
sealed interface Disponibilidad {
    data object Consultando : Disponibilidad
    data class ConStock(val unidades: Int, val diasEnvio: Int) : Disponibilidad
    data class SinStock(val diasEnvio: Int) : Disponibilidad   // D-3: se puede pedir igual
    data object NoEnCatalogo : Disponibilidad
    data object SinRespuesta : Disponibilidad
}

fun disponibilidadDe(stock: Int, diasEnvio: Int): Disponibilidad =
    if (stock > 0) Disponibilidad.ConStock(stock, diasEnvio) else Disponibilidad.SinStock(diasEnvio)

/** RF-1122 y D-9: los días que lleva esperando, desde la solicitud hasta la entrega o hasta hoy. */
fun diasDeEspera(solicitud: SolicitudRepuesto, ahora: Instant): Long =
    ChronoUnit.DAYS.between(solicitud.fechaSolicitud, solicitud.entregadaEn ?: ahora).coerceAtLeast(0)

/**
 * RF-1124 y D-7: al entregarse un repuesto, la incidencia vuelve a «En atención» solo si estaba esperando repuesto
 * y ya no queda ninguno por entregar. `null` = la marca no cambia.
 */
fun marcaAlEntregar(actual: MarcaSeguimiento, quedanSinEntregar: Int): MarcaSeguimiento? =
    if (actual == MarcaSeguimiento.ESPERANDO_REPUESTO && quedanSinEntregar == 0) MarcaSeguimiento.EN_ATENCION else null

/** RF-1120 y RF-1122: el estado que se muestra, que suma lo que pasa en el celular a lo que informa Logística. */
enum class EstadoSolicitudVisible { POR_ENVIAR, POR_DESPACHAR, EN_CAMINO, ENTREGADA, RECHAZADA }

fun estadoVisible(solicitud: SolicitudRepuesto): EstadoSolicitudVisible = when {
    solicitud.rechazadaPorLogistica -> EstadoSolicitudVisible.RECHAZADA
    solicitud.numeroLogistica == null -> EstadoSolicitudVisible.POR_ENVIAR
    else -> when (solicitud.estadoEntrega) {
        EstadoEntrega.POR_DESPACHAR -> EstadoSolicitudVisible.POR_DESPACHAR
        EstadoEntrega.EN_CAMINO -> EstadoSolicitudVisible.EN_CAMINO
        EstadoEntrega.ENTREGADO -> EstadoSolicitudVisible.ENTREGADA
    }
}
