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

    // RF-612 (HU-06): el técnico elegido, con la carga y el límite que bajaron del servidor
    @Query("SELECT * FROM tecnicos WHERE id = :id")
    suspend fun tecnicoPorId(id: String): TecnicoEntity?

    @Upsert
    suspend fun guardarTecnicos(tecnicos: List<TecnicoEntity>)

    // RF-619 (HU-06): quien deja de ser técnico o se elimina ya no se puede elegir
    @Query("DELETE FROM tecnicos WHERE id IN (:ids)")
    suspend fun borrarTecnicos(ids: List<String>)

    @Query("SELECT * FROM equipos WHERE codigo = :codigo")
    suspend fun equipoPorCodigo(codigo: String): EquipoEntity?

    @Upsert
    suspend fun guardarEquipos(equipos: List<EquipoEntity>)
}