package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * HU-14 (P-7 del plan 014): las sugerencias de IA en Room. Solo escribe en Room; subir las decididas es de `core/sync`
 * (RF-1429).
 */
interface SugerenciaIARepository {
    /** La más reciente sin decidir de ese tipo, o null (RF-1405, RF-1424). */
    fun observarSinDecidir(incidenciaId: String, tipo: TipoSugerencia): Flow<SugerenciaIA?>

    suspend fun sinDecidir(incidenciaId: String, tipo: TipoSugerencia): Resultado<SugerenciaIA?>

    /** Cuántas hay de ese tipo, decididas o no (RF-1405: la clasificación se pide una vez por celular). */
    suspend fun contar(incidenciaId: String, tipo: TipoSugerencia): Resultado<Int>

    suspend fun guardar(sugerencia: SugerenciaIA): Resultado<Unit>

    /** Registra la decisión una sola vez y la deja lista para subir (RF-1413, RF-1420, RF-1421). */
    suspend fun decidir(id: String, aceptada: Boolean, decididaPorId: String, ahora: Instant): Resultado<Unit>
}
