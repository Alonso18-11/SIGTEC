package com.softcorp.sigtec.core.domain.model

enum class TipoSugerencia { CLASIFICACION, SOLUCIONES_PARECIDAS, BORRADOR_INFORME }

data class SugerenciaIA(
    val id: String,
    val incidenciaId: String,
    val tipo: TipoSugerencia,
    val contenido: String,
    val aceptada: Boolean? = null    // null = todavía nadie la aceptó ni la descartó
)