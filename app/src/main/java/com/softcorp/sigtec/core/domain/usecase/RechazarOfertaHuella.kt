package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import javax.inject.Inject

class RechazarOfertaHuella @Inject constructor(
    private val preferencias: PreferenciasSeguridadRepository
) {
    suspend operator fun invoke() = preferencias.marcarOfrecida()
}