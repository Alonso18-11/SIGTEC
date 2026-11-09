package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.FallaDiccionarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FallaDiccionarioDao {
    // La búsqueda y el filtro se hacen en Kotlin para ignorar tildes (RF-1304 de la HU-13)
    @Query("SELECT * FROM fallas_diccionario ORDER BY titulo")
    fun observarTodas(): Flow<List<FallaDiccionarioEntity>>

    @Query("SELECT * FROM fallas_diccionario WHERE id = :id")
    suspend fun porId(id: String): FallaDiccionarioEntity?

    // RF-1319: «Ya está en el diccionario» en el detalle de la incidencia cerrada
    @Query("SELECT EXISTS(SELECT 1 FROM fallas_diccionario WHERE incidenciaOrigenId = :incidenciaId)")
    fun existeConOrigen(incidenciaId: String): Flow<Boolean>

    @Upsert
    suspend fun guardar(falla: FallaDiccionarioEntity)

    @Upsert
    suspend fun guardarTodas(fallas: List<FallaDiccionarioEntity>)

    @Query("SELECT * FROM fallas_diccionario WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<FallaDiccionarioEntity>

    // RF-1331: suman a los «registros sin enviar» del aviso de cerrar sesión
    @Query("SELECT COUNT(*) FROM fallas_diccionario WHERE pendienteSincronizar = 1")
    fun observarCantidadPendientes(): Flow<Int>

    @Query("UPDATE fallas_diccionario SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: String)
}
