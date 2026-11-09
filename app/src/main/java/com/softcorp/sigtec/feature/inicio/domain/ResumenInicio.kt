package com.softcorp.sigtec.feature.inicio.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import com.softcorp.sigtec.feature.atencion.domain.SolicitudConIncidencia
import com.softcorp.sigtec.feature.atencion.domain.estadoVisible

/** Qué Inicio ve cada persona; se decide por permisos, nunca comparando el perfil (CLAUDE.md). */
enum class VistaInicio { TECNICO, JEFE, REGISTRADOR }

/** Las métricas de la cabecera (RF-I4 a RF-I6). El texto lo pone la pantalla. */
enum class TipoMetrica { ASIGNADAS, EN_ATENCION, ESPERANDO_REPUESTO, SIN_ASIGNAR, CRITICAS, PENDIENTES, SOLUCIONADAS }

data class Metrica(val tipo: TipoMetrica, val valor: Int)

/** RF-I8: la pieza que el técnico espera antes. */
data class RepuestoProximo(val pieza: String, val codigoIncidencia: String?, val estado: EstadoSolicitudVisible)

data class ResumenInicio(
    val vista: VistaInicio,
    val metricas: List<Metrica>,
    val atendiendo: Incidencia? = null,        // RF-I7
    val repuesto: RepuestoProximo? = null,     // RF-I8
    val puedeRegistrar: Boolean = false        // RF-I9
)

fun vistaPara(usuario: Usuario): VistaInicio = when {
    usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) -> VistaInicio.TECNICO
    usuario.perfil.puede(Permiso.ASIGNAR_INCIDENCIA) -> VistaInicio.JEFE
    else -> VistaInicio.REGISTRADOR
}

/**
 * Inicio (specs/inicio-panel): calcula lo que muestra la pantalla a partir de lo que hay en Room. Función pura, para
 * probarla en la JVM. [solicitudes] solo importa para el técnico.
 */
fun calcularResumen(
    usuario: Usuario,
    incidencias: List<Incidencia>,
    solicitudes: List<SolicitudConIncidencia> = emptyList()
): ResumenInicio {
    val vista = vistaPara(usuario)
    val puedeRegistrar = usuario.perfil.puede(Permiso.REGISTRAR_INCIDENCIA)
    fun cuenta(lista: List<Incidencia>, condicion: (Incidencia) -> Boolean) = lista.count(condicion)
    return when (vista) {
        VistaInicio.TECNICO -> {
            // RF-I4: solo las asignadas a él que siguen pendientes
            val mias = incidencias.filter { it.tecnicoAsignadoId == usuario.id && it.marca != MarcaSeguimiento.CERRADA }
            ResumenInicio(
                vista = vista,
                metricas = listOf(
                    Metrica(TipoMetrica.ASIGNADAS, cuenta(mias) { it.marca == MarcaSeguimiento.ASIGNADA }),
                    Metrica(TipoMetrica.EN_ATENCION, cuenta(mias) { it.marca == MarcaSeguimiento.EN_ATENCION }),
                    Metrica(TipoMetrica.ESPERANDO_REPUESTO, cuenta(mias) { it.marca == MarcaSeguimiento.ESPERANDO_REPUESTO })
                ),
                // RF-I7: la más antigua en atención
                atendiendo = mias.filter { it.marca == MarcaSeguimiento.EN_ATENCION }.minByOrNull { it.fechaRegistro },
                repuesto = repuestoProximo(solicitudes),
                puedeRegistrar = puedeRegistrar
            )
        }
        // RF-I5: todo el área
        VistaInicio.JEFE -> ResumenInicio(
            vista = vista,
            metricas = listOf(
                Metrica(TipoMetrica.SIN_ASIGNAR, cuenta(incidencias) { it.marca == MarcaSeguimiento.SIN_ASIGNAR }),
                Metrica(TipoMetrica.EN_ATENCION, cuenta(incidencias) { it.marca == MarcaSeguimiento.EN_ATENCION }),
                Metrica(TipoMetrica.CRITICAS, cuenta(incidencias) { it.critica && it.marca != MarcaSeguimiento.CERRADA })
            ),
            puedeRegistrar = puedeRegistrar
        )
        // RF-I6: lo que registró esta persona
        VistaInicio.REGISTRADOR -> {
            val suyas = incidencias.filter { it.registradoPorId == usuario.id }
            ResumenInicio(
                vista = vista,
                metricas = listOf(
                    Metrica(TipoMetrica.PENDIENTES, cuenta(suyas) { it.marca != MarcaSeguimiento.CERRADA }),
                    Metrica(TipoMetrica.EN_ATENCION, cuenta(suyas) {
                        it.marca == MarcaSeguimiento.EN_ATENCION || it.marca == MarcaSeguimiento.ESPERANDO_REPUESTO
                    }),
                    Metrica(TipoMetrica.SOLUCIONADAS, cuenta(suyas) { it.marca == MarcaSeguimiento.CERRADA })
                ),
                puedeRegistrar = puedeRegistrar
            )
        }
    }
}

/** RF-I8: sin entregar ni rechazadas; primero la de llegada estimada más cercana y, si ninguna la tiene, la más antigua. */
internal fun repuestoProximo(solicitudes: List<SolicitudConIncidencia>): RepuestoProximo? =
    solicitudes
        .filter {
            val estado = estadoVisible(it.solicitud)
            estado != EstadoSolicitudVisible.ENTREGADA && estado != EstadoSolicitudVisible.RECHAZADA
        }
        .sortedWith(compareBy(nullsLast()) { it: SolicitudConIncidencia -> it.solicitud.llegadaEstimada }
            .thenBy { it.solicitud.fechaSolicitud })
        .firstOrNull()
        ?.let { RepuestoProximo(it.solicitud.pieza, it.codigoIncidencia, estadoVisible(it.solicitud)) }

/** RF-I1: el saludo según la hora del celular. */
fun saludo(hora: Int): String = when {
    hora < 12 -> "Buenos días"
    hora < 19 -> "Buenas tardes"
    else -> "Buenas noches"
}
