package com.softcorp.sigtec.core.domain.model

import java.time.Instant

enum class TipoMovimiento {
    REGISTRADA, ASIGNADA, REASIGNADA, ATENCION_INICIADA,
    REPUESTO_SOLICITADO, REPUESTO_RECIBIDO, CERRADA
}

data class Movimiento(
    val id: String,
    val incidenciaId: String,
    val tipo: TipoMovimiento,
    val autorNombre: String,
    val fecha: Instant,
    val detalle: String? = null      // "Por Carlos Mendoza, jefe del área"
)