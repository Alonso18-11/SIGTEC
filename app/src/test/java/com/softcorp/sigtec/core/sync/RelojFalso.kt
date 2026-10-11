package com.softcorp.sigtec.core.sync

import java.time.Instant

// Doble de prueba de Reloj: devuelve siempre el mismo instante fijo.
// Preparado para cuando exista una prueba completa de SincronizacionTiempoReal
// o SincronizadorIncidencias (ver plan.md sección 8); todavía no se usa.
class RelojFalso(private val instante: Instant) : Reloj {
    override fun ahora(): Instant = instante
}
