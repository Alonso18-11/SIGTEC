package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservarPreferenciasHuella @Inject constructor(
    private val preferencias: PreferenciasSeguridadRepository
) {
    operator fun invoke(): Flow<PreferenciasHuella> = preferencias.preferenciasHuella
}