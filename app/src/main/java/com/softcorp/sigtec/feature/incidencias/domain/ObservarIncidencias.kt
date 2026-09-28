package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** HU-04: la lista general, leída siempre de Room. */
class ObservarIncidencias @Inject constructor(private val repo: IncidenciaRepository) {
    operator fun invoke(): Flow<List<Incidencia>> = repo.observarTodas()
}
