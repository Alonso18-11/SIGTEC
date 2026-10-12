package com.softcorp.sigtec.core.domain.model

import java.time.Instant

data class InformeTecnico(
    val id: String,
    val incidenciaId: String,
    val diagnostico: String,         // causa del problema (RF-813, RF-827)
    val trabajoRealizado: String,
    val piezasCambiadas: String?,
    val observaciones: String?,
    val fechaCierre: Instant,
    val confirmadoEn: Instant        // obligatorio: sin huella no hay informe (RF-23)
)