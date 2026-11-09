package com.softcorp.sigtec.feature.diccionario.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** HU-13 (RF-1319): si ya se guardó en el diccionario una falla desde esa incidencia. Solo lee Room. */
class EstaEnDiccionario @Inject constructor(private val repo: DiccionarioRepository) {
    operator fun invoke(incidenciaId: String): Flow<Boolean> = repo.existeConOrigen(incidenciaId)
}
