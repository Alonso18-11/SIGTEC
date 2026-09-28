package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import javax.inject.Inject

class DesbloquearApp @Inject constructor(private val bloqueo: BloqueoApp) {
    operator fun invoke() = bloqueo.desbloquear()
}