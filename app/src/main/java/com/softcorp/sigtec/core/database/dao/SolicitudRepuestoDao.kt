package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.SolicitudRepuestoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SolicitudRepuestoDao {
    @Query("SELECT * FROM solicitudes_repuesto ORDER BY fechaSolicitud DESC")
    fun observarTodas(): Flow<List<SolicitudRepuestoEntity>>

    @Query("SELECT * FROM solicitudes_repuesto WHERE incidenciaId = :incidenciaId")
    fun observarPorIncidencia(incidenciaId: String): Flow<List<SolicitudRepuestoEntity>>

    // Para el worker que consulta a Logística una vez al día (HU-18)
    @Query("SELECT * FROM solicitudes_repuesto WHERE estadoEntrega != 'ENTREGADO'")
    suspend fun sinEntregar(): List<SolicitudRepuestoEntity>

    @Upsert
    suspend fun guardar(solicitud: SolicitudRepuestoEntity)

    @Query("SELECT * FROM solicitudes_repuesto WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<SolicitudRepuestoEntity>
}