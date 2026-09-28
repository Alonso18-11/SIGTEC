package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.InformeTecnicoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InformeTecnicoDao {
    @Query("SELECT * FROM informes_tecnicos WHERE incidenciaId = :incidenciaId")
    fun observarPorIncidencia(incidenciaId: String): Flow<InformeTecnicoEntity?>

    @Upsert
    suspend fun guardar(informe: InformeTecnicoEntity)

    @Query("SELECT * FROM informes_tecnicos WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<InformeTecnicoEntity>
}