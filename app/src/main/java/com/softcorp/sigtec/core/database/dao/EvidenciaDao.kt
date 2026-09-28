package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.EvidenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaDao {
    @Query("SELECT * FROM evidencias WHERE incidenciaId = :incidenciaId ORDER BY fechaCaptura")
    fun observarPorIncidencia(incidenciaId: String): Flow<List<EvidenciaEntity>>

    @Upsert
    suspend fun guardar(evidencia: EvidenciaEntity)

    @Delete
    suspend fun eliminar(evidencia: EvidenciaEntity)
}