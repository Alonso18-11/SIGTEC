package com.softcorp.sigtec.core.domain.model

import java.time.Instant

/** HU-05: qué tan al día están los datos que se muestran. */
data class EstadoDatos(
    val enLinea: Boolean,
    val ultimaDescarga: Instant?,
    val pendientesPorEnviar: Int,
    val necesitanAtencion: Int,
    val fotosEnCelular: Int = 0   // HU-09 (RF-943): se pierden al borrar los datos locales
)
