package com.softcorp.sigtec.core.domain.model

import java.time.Instant

/** Los dos estados oficiales del caso. Nunca se guardan: se derivan de la marca. */
enum class EstadoIncidencia { PENDIENTE, SOLUCIONADO }

/** Punto de la atención en que está el caso (sección 7 del documento). */
enum class MarcaSeguimiento {
    SIN_ASIGNAR, ASIGNADA, EN_ATENCION, ESPERANDO_REPUESTO, CERRADA
}

enum class TipoFalla { HARDWARE, SOFTWARE, RED }

enum class Prioridad { BAJA, MEDIA, ALTA }

data class Incidencia(
    val id: String,                      // UUID generado en el celular
    val numero: Int?,                    // null hasta sincronizar (ver decisión 3)
    val codigoEquipo: String,
    val descripcion: String,
    val usuarioResponsable: String,
    val fechaRegistro: Instant,
    val registradoPorId: String,
    val registradoPorNombre: String,
    val marca: MarcaSeguimiento,
    val tipoFalla: TipoFalla? = null,     // lo sugiere la IA y lo confirma el jefe
    val prioridad: Prioridad? = null,
    val tecnicoAsignadoId: String? = null,
    val tecnicoAsignadoNombre: String? = null,
    val critica: Boolean = false          // la marca el worker diario (HU-18)
) {
    val estado: EstadoIncidencia
        get() = if (marca == MarcaSeguimiento.CERRADA) EstadoIncidencia.SOLUCIONADO
        else EstadoIncidencia.PENDIENTE

    /** INC-0124, o null si aún no se sincronizó */
    val codigoVisible: String?
        get() = numero?.let { "INC-%04d".format(it) }
}