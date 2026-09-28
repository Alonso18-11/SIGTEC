package com.softcorp.sigtec.core.domain.model

import java.time.Instant

/** HU-05: qué tan al día están los datos que se muestran. */
data class EstadoDatos(
    val enLinea: Boolean,
    val ultimaDescarga: Instant?,
    val pendientesPorEnviar: Int
)
