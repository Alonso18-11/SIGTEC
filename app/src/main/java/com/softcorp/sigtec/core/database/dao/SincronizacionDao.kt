package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SincronizacionDao {

    @Insert
    suspend fun registrar(ejecucion: SincronizacionEntity)

    // "Sin conexión · mostrando lo descargado a las 09:40" (HU-05)
    @Query("""
        SELECT * FROM sincronizaciones
        WHERE tipoTarea = :tipo AND exitosa = 1
        ORDER BY ejecutadaEn DESC LIMIT 1
    """)
    fun observarUltimaExitosa(tipo: String): Flow<SincronizacionEntity?>

    // Conserva solo las 20 ejecuciones más recientes de cada tarea
    @Query("""
        DELETE FROM sincronizaciones WHERE tipoTarea = :tipo AND id NOT IN (
            SELECT id FROM sincronizaciones WHERE tipoTarea = :tipo
            ORDER BY ejecutadaEn DESC LIMIT 20
        )
    """)
    suspend fun recortar(tipo: String)
}
