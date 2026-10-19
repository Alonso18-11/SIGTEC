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

    @Query("""
        SELECT inf.* FROM informes_tecnicos inf LEFT JOIN incidencias i ON i.id = inf.incidenciaId
        WHERE inf.pendienteSincronizar = 1
          AND (i.id IS NULL OR (i.pendienteSincronizar = 0 AND i.necesitaAtencion = 0))
    """)
    suspend fun listosParaSubir(): List<InformeTecnicoEntity>

    @Query("SELECT * FROM informes_tecnicos WHERE id = :id")
    suspend fun porId(id: String): InformeTecnicoEntity?

    @Query("UPDATE informes_tecnicos SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    @Query("""
        UPDATE informes_tecnicos SET pendienteSincronizar = 0, necesitaAtencion = 1
        WHERE id = :id
    """)
    suspend fun marcarNecesitaAtencion(id: String)

    // RF-825: los informes pendientes de una incidencia rechazada ya nunca se suben
    @Query("""
        UPDATE informes_tecnicos SET pendienteSincronizar = 0, necesitaAtencion = 1
        WHERE incidenciaId = :incidenciaId AND pendienteSincronizar = 1
    """)
    suspend fun marcarNecesitaAtencionDe(incidenciaId: String)

    // RF-825: los rechazados nunca llegaron al servidor. Sin la rama de "incidencia sin número" que sí
    // tiene MovimientoDao.borrarRechazados(): un informe no puede pertenecer a una incidencia sin número.
    @Query("DELETE FROM informes_tecnicos WHERE necesitaAtencion = 1")
    suspend fun borrarRechazados()
}