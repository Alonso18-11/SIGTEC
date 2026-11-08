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

    // RF-1118: como el informe técnico, solo sube cuando su incidencia ya está en el servidor
    @Query("""
        SELECT s.* FROM solicitudes_repuesto s LEFT JOIN incidencias i ON i.id = s.incidenciaId
        WHERE s.pendienteSincronizar = 1 AND s.necesitaAtencion = 0
          AND (i.id IS NULL OR (i.pendienteSincronizar = 0 AND i.necesitaAtencion = 0))
    """)
    suspend fun listosParaSubir(): List<SolicitudRepuestoEntity>

    @Query("SELECT COUNT(*) FROM solicitudes_repuesto WHERE pendienteSincronizar = 1")
    fun observarPendientes(): Flow<Int>

    // Si cambió mientras se subía (por ejemplo, llegó el número de Logística), sigue pendiente
    @Query("""
        UPDATE solicitudes_repuesto SET pendienteSincronizar = 0
        WHERE id = :id AND actualizadoEn = :actualizadoEn
    """)
    suspend fun marcarSincronizada(id: String, actualizadoEn: Instant): Int

    // Firestore la rechazó de forma permanente: ya no se reintenta, como el informe técnico (RF-825)
    @Query("UPDATE solicitudes_repuesto SET pendienteSincronizar = 0, necesitaAtencion = 1 WHERE id = :id")
    suspend fun marcarNecesitaAtencion(id: String)
}
