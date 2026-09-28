package com.softcorp.sigtec.core.domain.model

import java.time.Instant

enum class EstadoEntrega { POR_DESPACHAR, EN_CAMINO, ENTREGADO }

data class SolicitudRepuesto(
    val id: String,
    val incidenciaId: String,
    val pieza: String,
    val motivo: String,
    val fechaSolicitud: Instant,
    val confirmadoEn: Instant,       // obligatorio, igual que el informe
    val estadoEntrega: EstadoEntrega
)