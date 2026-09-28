package com.softcorp.sigtec.core.domain.model

import java.time.Instant

enum class TipoEvidencia { FALLA, PIEZA_CAMBIADA }

data class Evidencia(
    val id: String,
    val incidenciaId: String,
    val tipo: TipoEvidencia,
    val rutaArchivo: String,         // almacenamiento interno (sección 16.1)
    val fechaCaptura: Instant
)