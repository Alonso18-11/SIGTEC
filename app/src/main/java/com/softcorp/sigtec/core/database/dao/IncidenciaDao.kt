package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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

    // RF-602 (HU-06): un técnico por cada asignación guardada en este celular que aún no subió. Solo
    // cuenta si su movimiento de asignación también está pendiente, para no contar dos veces una
    // incidencia que ya está asignada en el servidor y quedó pendiente por otro motivo.
    @Query("""
        SELECT i.tecnicoAsignadoId FROM incidencias i
        WHERE i.tecnicoAsignadoId IS NOT NULL AND i.marca != 'CERRADA' AND i.pendienteSincronizar = 1
          AND EXISTS (
              SELECT 1 FROM movimientos m
              WHERE m.incidenciaId = i.id AND m.pendienteSincronizar = 1
                AND m.tipo IN ('ASIGNADA', 'REASIGNADA')
          )
    """)
    fun observarAsignacionesSinSubir(): Flow<List<String>>

    // RF-612 (HU-06): lo mismo, para un técnico, al momento de asignar
    @Query("""
        SELECT COUNT(*) FROM incidencias i
        WHERE i.tecnicoAsignadoId = :tecnicoId AND i.marca != 'CERRADA' AND i.pendienteSincronizar = 1
          AND EXISTS (
              SELECT 1 FROM movimientos m
              WHERE m.incidenciaId = i.id AND m.pendienteSincronizar = 1
                AND m.tipo IN ('ASIGNADA', 'REASIGNADA')
          )
    """)
    suspend fun asignacionesSinSubir(tecnicoId: String): Int

    // Lo que el worker debe subir a Firestore (HU-17)
    @Query("SELECT * FROM incidencias WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<IncidenciaEntity>

    // --- Sincronización (Alonso, HU-05) ---

    @Query("SELECT * FROM incidencias WHERE id = :id")
    suspend fun porId(id: String): IncidenciaEntity?

    @Query("UPDATE incidencias SET numero = :numero WHERE id = :id")
    suspend fun asignarNumero(id: String, numero: Int)

    // Solo se marca como subida si nadie la modificó mientras se subía
    @Query("""
        UPDATE incidencias SET pendienteSincronizar = 0
        WHERE id = :id AND actualizadoEn = :actualizadoEn
    """)
    suspend fun marcarSincronizada(id: String, actualizadoEn: Instant): Int

    @Query("SELECT COUNT(*) FROM incidencias WHERE pendienteSincronizar = 1")
    fun observarPendientes(): Flow<Int>

    // RF-75: igual que marcarSincronizada, no marca una versión que se editó mientras se subía
    @Query("""
        UPDATE incidencias SET pendienteSincronizar = 0, necesitaAtencion = 1
        WHERE id = :id AND actualizadoEn = :actualizadoEn
    """)
    suspend fun marcarNecesitaAtencion(id: String, actualizadoEn: Instant): Int

    // RF-76: incidencias afectadas por un rechazo, propio o de alguno de sus movimientos;
    // la unión cuenta una sola vez la incidencia rechazada junto con sus movimientos
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT id FROM incidencias WHERE necesitaAtencion = 1
            UNION
            SELECT incidenciaId FROM movimientos WHERE necesitaAtencion = 1
        )
    """)
    fun observarAfectadasPorRechazo(): Flow<Int>

    // RF-80: una rechazada sin número nunca llegó al servidor; se descarta del celular
    @Query("DELETE FROM incidencias WHERE necesitaAtencion = 1 AND numero IS NULL")
    suspend fun borrarRechazadasSinNumero()

    // RF-80: las demás ya muestran la versión del servidor; solo queda quitar la marca
    @Query("UPDATE incidencias SET necesitaAtencion = 0 WHERE necesitaAtencion = 1")
    suspend fun quitarMarcaDeAtencion()
}
