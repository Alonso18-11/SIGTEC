package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import javax.inject.Inject

/** Vuelve al acceso conservando la sesión: el dueño reentra con su huella. */
class BloquearApp @Inject constructor(private val bloqueo: BloqueoApp) {
    operator fun invoke() = bloqueo.bloquear()
}