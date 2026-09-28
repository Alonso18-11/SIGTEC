package com.softcorp.sigtec.core.domain.model

data class FallaDiccionario(
    val id: String,
    val titulo: String,
    val sintomas: String,
    val componente: String,          // Fuente de poder, Impresora, Office...
    val tipo: TipoFalla,             // filtros Hardware / Software / Red del prototipo
    val solucion: String,
    val autorNombre: String
)