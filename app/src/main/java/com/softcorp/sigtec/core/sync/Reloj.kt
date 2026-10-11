package com.softcorp.sigtec.core.sync

import java.time.Instant
import javax.inject.Inject

// RNF «Reloj inyectado»: abstrae el reloj del sistema para poder probar
// las clases de core/sync con un instante fijo (ver RelojFalso en pruebas).
interface Reloj {
    fun ahora(): Instant
}

class RelojSistema @Inject constructor() : Reloj {
    override fun ahora(): Instant = Instant.now()
}
