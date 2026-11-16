package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * HU-14 (RF-1405, RF-1424): la sugerencia sin decidir más reciente de una incidencia. Al volver a la pantalla se ve la
 * misma, sin pedir otra a Gemini. Solo lee lo guardado en este celular; no hay nada que verificar.
 */
class ObservarSugerencia @Inject constructor(private val repo: SugerenciaIARepository) {
    operator fun invoke(incidenciaId: String, tipo: TipoSugerencia): Flow<SugerenciaIA?> =
        repo.observarSinDecidir(incidenciaId, tipo)
}
