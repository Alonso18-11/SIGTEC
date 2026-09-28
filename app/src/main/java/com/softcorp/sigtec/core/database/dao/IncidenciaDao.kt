package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenciaDao {

    // Lista general: de la más reciente a la más antigua (HU-04)
    @Query("SELECT * FROM incidencias ORDER BY fechaRegistro DESC")
    fun observarTodas(): Flow<List<IncidenciaEntity>>

    // Bandeja del técnico: solo las suyas sin cerrar, de la más antigua a la más reciente (HU-08)
    @Query("""
        SELECT * FROM incidencias
        WHERE tecnicoAsignadoId = :tecnicoId AND marca != 'CERRADA'
        ORDER BY fechaRegistro ASC
    """)
    fun observarBandeja(tecnicoId: String): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE id = :id")
    fun observarPorId(id: String): Flow<IncidenciaEntity?>

    // Historial del equipo, en orden cronológico (HU-16)
    @Query("SELECT * FROM incidencias WHERE codigoEquipo = :codigo ORDER BY fechaRegistro DESC")
    fun observarPorEquipo(codigo: String): Flow<List<IncidenciaEntity>>

    @Upsert
    suspend fun guardar(incidencia: IncidenciaEntity)

    @Upsert
    suspend fun guardarTodas(incidencias: List<IncidenciaEntity>)

    // Lo que el worker debe subir a Firestore (HU-17)
    @Query("SELECT * FROM incidencias WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<IncidenciaEntity>
}