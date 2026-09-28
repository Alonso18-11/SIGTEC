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

    // --- Sincronización (Alonso, HU-05) ---

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun porId(id: String): MovimientoEntity?

    @Query("UPDATE movimientos SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    @Query("SELECT COUNT(*) FROM movimientos WHERE pendienteSincronizar = 1")
    fun observarPendientes(): Flow<Int>
}
