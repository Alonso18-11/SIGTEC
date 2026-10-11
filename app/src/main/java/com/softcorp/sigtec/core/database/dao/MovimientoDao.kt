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

    // RF-68: solo los que ya pueden subir, porque su incidencia quedó en el servidor (o no está en el
    // celular). Así un movimiento nunca llega a Firestore si su incidencia termina rechazada.
    @Query("""
        SELECT m.* FROM movimientos m LEFT JOIN incidencias i ON i.id = m.incidenciaId
        WHERE m.pendienteSincronizar = 1
          AND (i.id IS NULL OR (i.pendienteSincronizar = 0 AND i.necesitaAtencion = 0))
    """)
    suspend fun listosParaSubir(): List<MovimientoEntity>

    // --- Sincronización (Alonso, HU-05) ---

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun porId(id: String): MovimientoEntity?

    @Query("UPDATE movimientos SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    @Query("SELECT COUNT(*) FROM movimientos WHERE pendienteSincronizar = 1")
    fun observarPendientes(): Flow<Int>

    @Query("""
        UPDATE movimientos SET pendienteSincronizar = 0, necesitaAtencion = 1
        WHERE id = :id
    """)
    suspend fun marcarNecesitaAtencion(id: String)

    // RF-75, RF-79: los pendientes de una incidencia rechazada ya nunca se suben
    @Query("""
        UPDATE movimientos SET pendienteSincronizar = 0, necesitaAtencion = 1
        WHERE incidenciaId = :incidenciaId AND pendienteSincronizar = 1
    """)
    suspend fun marcarNecesitaAtencionDe(incidenciaId: String)
}
