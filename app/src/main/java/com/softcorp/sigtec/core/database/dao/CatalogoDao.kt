package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.EquipoEntity
import com.softcorp.sigtec.core.database.entity.TecnicoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {
    @Query("SELECT * FROM tecnicos ORDER BY nombre")
    fun observarTecnicos(): Flow<List<TecnicoEntity>>

    @Upsert
    suspend fun guardarTecnicos(tecnicos: List<TecnicoEntity>)

    @Query("SELECT * FROM equipos WHERE codigo = :codigo")
    suspend fun equipoPorCodigo(codigo: String): EquipoEntity?

    @Upsert
    suspend fun guardarEquipos(equipos: List<EquipoEntity>)
}