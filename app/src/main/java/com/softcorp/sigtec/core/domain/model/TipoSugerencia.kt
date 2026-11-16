package com.softcorp.sigtec.core.domain.model

import java.time.Instant

// Las «Fallas parecidas» de la HU-13 se calculan en el celular y no se guardan (D-1 de la spec 013): no son un tipo
enum class TipoSugerencia { CLASIFICACION, BORRADOR_INFORME }

/**
 * Lo que propuso la IA y lo que la persona decidió (HU-14, RF-1427). La IA solo sugiere: [aceptada] queda en null
 * hasta que alguien la usa o la descarta, y entonces se guardan quién decidió y cuándo.
 */
data class SugerenciaIA(
    val id: String,
    val incidenciaId: String,
    val tipo: TipoSugerencia,
    val contenido: String,              // tal como se mostró (JSON canónico, P-5 del plan 014)
    val modelo: String,                 // el modelo de Gemini que la generó
    val generadaEn: Instant,
    val aceptada: Boolean? = null,      // null = todavía nadie la aceptó ni la descartó
    val decididaPorId: String? = null,
    val decididaEn: Instant? = null
)
