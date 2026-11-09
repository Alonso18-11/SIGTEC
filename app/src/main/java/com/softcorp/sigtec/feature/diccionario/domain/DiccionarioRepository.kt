package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import kotlinx.coroutines.flow.Flow

/** HU-13: el diccionario se lee y se escribe solo en Room; `core/sync` lo sube y lo baja. */
interface DiccionarioRepository {
    /** Todas las fallas del celular, ordenadas por título (RF-1301). */
    fun observarTodas(): Flow<List<FallaDiccionario>>

    /** RF-1319: si ya hay una falla guardada desde esa incidencia. */
    fun existeConOrigen(incidenciaId: String): Flow<Boolean>

    /** RF-1313: guarda con `pendienteSincronizar = true`. */
    suspend fun guardar(falla: FallaDiccionario): Resultado<Unit>
}
