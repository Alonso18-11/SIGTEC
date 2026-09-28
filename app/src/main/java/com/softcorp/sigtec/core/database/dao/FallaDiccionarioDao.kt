package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.FallaDiccionarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FallaDiccionarioDao {
    @Query("SELECT * FROM fallas_diccionario ORDER BY titulo")
    fun observarTodas(): Flow<List<FallaDiccionarioEntity>>

    // Búsqueda por palabra clave o síntoma (HU-13)
    @Query("""
        SELECT * FROM fallas_diccionario
        WHERE titulo LIKE '%' || :texto || '%'
           OR sintomas LIKE '%' || :texto || '%'
           OR componente LIKE '%' || :texto || '%'
        ORDER BY titulo
    """)
    fun buscar(texto: String): Flow<List<FallaDiccionarioEntity>>

    @Upsert
    suspend fun guardar(falla: FallaDiccionarioEntity)

    @Upsert
    suspend fun guardarTodas(fallas: List<FallaDiccionarioEntity>)

    @Query("SELECT * FROM fallas_diccionario WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<FallaDiccionarioEntity>
}