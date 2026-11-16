package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.SugerenciaIAEntity
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SugerenciaIADao {
    @Query("SELECT * FROM sugerencias_ia WHERE incidenciaId = :incidenciaId")
    suspend fun porIncidencia(incidenciaId: String): List<SugerenciaIAEntity>

    // HU-14 (RF-1405, RF-1424): la más reciente sin decidir; al reabrir la pantalla se muestra sin volver a pedirla
    @Query(
        "SELECT * FROM sugerencias_ia WHERE incidenciaId = :incidenciaId AND tipo = :tipo AND aceptada IS NULL " +
            "ORDER BY generadaEn DESC LIMIT 1"
    )
    fun observarSinDecidir(incidenciaId: String, tipo: TipoSugerencia): Flow<SugerenciaIAEntity?>

    @Query(
        "SELECT * FROM sugerencias_ia WHERE incidenciaId = :incidenciaId AND tipo = :tipo AND aceptada IS NULL " +
            "ORDER BY generadaEn DESC LIMIT 1"
    )
    suspend fun sinDecidir(incidenciaId: String, tipo: TipoSugerencia): SugerenciaIAEntity?

    // RF-1405: decidida o no, una clasificación ya pedida en este celular no se vuelve a pedir
    @Query("SELECT COUNT(*) FROM sugerencias_ia WHERE incidenciaId = :incidenciaId AND tipo = :tipo")
    suspend fun contar(incidenciaId: String, tipo: TipoSugerencia): Int

    @Upsert
    suspend fun guardar(sugerencia: SugerenciaIAEntity)

    // RF-1413, RF-1420, RF-1421: una vez decidida no cambia (como en Firestore); queda lista para subir (RF-1429)
    @Query(
        "UPDATE sugerencias_ia SET aceptada = :aceptada, decididaPorId = :decididaPorId, decididaEn = :decididaEn, " +
            "actualizadoEn = :decididaEn, pendienteSincronizar = 1 WHERE id = :id AND aceptada IS NULL"
    )
    suspend fun decidir(id: String, aceptada: Boolean, decididaPorId: String, decididaEn: Instant): Int

    // RF-1429 (D-7): solo se suben las decididas
    @Query("SELECT * FROM sugerencias_ia WHERE pendienteSincronizar = 1 AND aceptada IS NOT NULL")
    suspend fun pendientesDeSincronizar(): List<SugerenciaIAEntity>

    @Query("SELECT COUNT(*) FROM sugerencias_ia WHERE pendienteSincronizar = 1 AND aceptada IS NOT NULL")
    fun observarCantidadPendientes(): Flow<Int>

    @Query("UPDATE sugerencias_ia SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: String)
}
