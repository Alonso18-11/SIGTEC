package com.softcorp.sigtec.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.softcorp.sigtec.core.database.entity.SolicitudRepuestoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SolicitudRepuestoDao {
    @Query("SELECT * FROM solicitudes_repuesto ORDER BY fechaSolicitud DESC")
    fun observarTodas(): Flow<List<SolicitudRepuestoEntity>>

    // RF-1120: la sección «Repuestos» del detalle, de la más reciente a la más antigua
    @Query("SELECT * FROM solicitudes_repuesto WHERE incidenciaId = :incidenciaId ORDER BY fechaSolicitud DESC")
    fun observarPorIncidencia(incidenciaId: String): Flow<List<SolicitudRepuestoEntity>>

    // RF-1121: «Estado de repuestos» muestra las que pidió el técnico
    @Query("SELECT * FROM solicitudes_repuesto WHERE solicitadoPorId = :tecnicoId ORDER BY fechaSolicitud DESC")
    fun observarDeTecnico(tecnicoId: String): Flow<List<SolicitudRepuestoEntity>>

    @Query("SELECT * FROM solicitudes_repuesto WHERE id = :id")
    suspend fun porId(id: String): SolicitudRepuestoEntity?

    // RF-1117: las que todavía no se registraron en Logística; las rechazadas ya no se reintentan (RF-1119)
    @Query("""
        SELECT * FROM solicitudes_repuesto
        WHERE numeroLogistica IS NULL AND rechazadaPorLogistica = 0
        ORDER BY fechaSolicitud
    """)
    suspend fun sinNumeroDeLogistica(): List<SolicitudRepuestoEntity>

    // RF-1123: las del técnico que Logística todavía no entregó
    @Query("""
        SELECT * FROM solicitudes_repuesto
        WHERE solicitadoPorId = :tecnicoId AND numeroLogistica IS NOT NULL
          AND rechazadaPorLogistica = 0 AND estadoEntrega != 'ENTREGADO'
    """)
    suspend fun sinEntregarDe(tecnicoId: String): List<SolicitudRepuestoEntity>

    // Para el worker que consulta a Logística una vez al día (HU-18)
    @Query("""
        SELECT * FROM solicitudes_repuesto
        WHERE numeroLogistica IS NOT NULL AND rechazadaPorLogistica = 0 AND estadoEntrega != 'ENTREGADO'
    """)
    suspend fun sinEntregar(): List<SolicitudRepuestoEntity>

    // RF-1124: la incidencia vuelve a «En atención» solo cuando no queda ninguna por entregar
    @Query("""
        SELECT COUNT(*) FROM solicitudes_repuesto
        WHERE incidenciaId = :incidenciaId AND rechazadaPorLogistica = 0 AND estadoEntrega != 'ENTREGADO'
    """)
    suspend fun contarSinEntregarDe(incidenciaId: String): Int

    @Upsert
    suspend fun guardar(solicitud: SolicitudRepuestoEntity)

    @Query("""
        UPDATE solicitudes_repuesto SET numeroLogistica = :numero, pendienteSincronizar = 1, actualizadoEn = :ahora
        WHERE id = :id
    """)
    suspend fun marcarRegistrada(id: String, numero: String, ahora: Instant)

    @Query("""
        UPDATE solicitudes_repuesto SET rechazadaPorLogistica = 1, pendienteSincronizar = 1, actualizadoEn = :ahora
        WHERE id = :id
    """)
    suspend fun marcarRechazada(id: String, ahora: Instant)

    @Query("SELECT * FROM solicitudes_repuesto WHERE pendienteSincronizar = 1")
    suspend fun pendientesDeSincronizar(): List<SolicitudRepuestoEntity>

    @Query("UPDATE solicitudes_repuesto SET pendienteSincronizar = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: String)
}
