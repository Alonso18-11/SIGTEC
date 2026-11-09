package com.softcorp.sigtec.core.domain.model

import java.time.Instant

/** HU-13: una falla resuelta con su solución. No se edita ni se borra (D-4 de la spec 013). */
data class FallaDiccionario(
    val id: String,
    val titulo: String,
    val sintomas: String,
    val componente: String,          // Fuente de poder, Impresora, Office...
    val tipo: TipoFalla,             // filtros Hardware / Software / Red del prototipo
    val solucion: String,
    val autorId: String,
    val autorNombre: String,
    val registradaEn: Instant,
    val incidenciaOrigenId: String? = null   // RF-1318: si se guardó desde una incidencia cerrada
)
