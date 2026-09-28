package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos WHERE incidenciaId = :incidenciaId ORDER BY fecha DESC")
    fun observarPorIncidencia(incidenciaId: String): Flow<List<MovimientoEntity>>

    @Upsert
    suspend fun guardar(movimiento: MovimientoEntity)

    @Query("SELECT * FROM movimientos WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<MovimientoEntity>
}