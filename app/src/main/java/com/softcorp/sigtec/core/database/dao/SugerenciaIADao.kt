package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.SugerenciaIAEntity

@Dao
interface SugerenciaIADao {
    @Query("SELECT * FROM sugerencias_ia WHERE incidenciaId = :incidenciaId")
    suspend fun porIncidencia(incidenciaId: String): List<SugerenciaIAEntity>

    @Query("UPDATE sugerencias_ia SET aceptada = :aceptada, pendienteSincronizar = 1 WHERE id = :id")
    suspend fun responder(id: String, aceptada: Boolean)

    @Upsert
    suspend fun guardar(sugerencia: SugerenciaIAEntity)
}