package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservarEstadoDatos @Inject constructor(private val repo: EstadoDatosRepository) {
    operator fun invoke(): Flow<EstadoDatos> = repo.estado
}
