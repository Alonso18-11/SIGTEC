package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObservarBandeja @Inject constructor(
    private val observarUsuarioActual: ObservarUsuarioActual,
    private val repo: AtencionRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Incidencia>> =
        observarUsuarioActual().flatMapLatest { usuario ->
            usuario?.let { repo.observarBandeja(it.id) } ?: flowOf(emptyList())
        }
}
