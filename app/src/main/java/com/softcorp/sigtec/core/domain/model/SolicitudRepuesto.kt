package com.softcorp.sigtec.core.domain.model

import java.time.Instant

enum class EstadoEntrega { POR_DESPACHAR, EN_CAMINO, ENTREGADO }

/** HU-11: una pieza pedida a Logística para una incidencia; puede haber varias por incidencia (RF-1103). */
data class SolicitudRepuesto(
    val id: String,
    val incidenciaId: String,
    val pieza: String,
    val motivo: String,
    val fechaSolicitud: Instant,
    val confirmadoEn: Instant,       // obligatorio, igual que el informe (RF-23 del producto)
    val estadoEntrega: EstadoEntrega,
    val solicitadoPorId: String,     // RF-1120: quién la confirmó con huella
    val solicitadoPorNombre: String,
    val numeroLogistica: String? = null,     // RF-1117: null mientras no se registra en Logística
    val stockConsultado: Int? = null,        // RF-1113: lo que vio el técnico al pedirla; null si no se consultó
    val diasEnvio: Int? = null,
    val despachadaEn: Instant? = null,       // RF-1122: los pasos que informa Logística
    val entregadaEn: Instant? = null,
    val llegadaEstimada: Instant? = null,
    val rechazadaPorLogistica: Boolean = false   // RF-1119: error permanente, ya no se reintenta
)
