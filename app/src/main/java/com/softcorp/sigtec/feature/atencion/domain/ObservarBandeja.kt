package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * HU-08 (RF-801, RF-803, RF-805): la bandeja del técnico con sesión actual, de la más antigua a la
 * más reciente; sin sesión, lista vacía (RF-803 la muestra como "sin pendientes" en la pantalla).
 */
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
