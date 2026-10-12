package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** HU-06: todo lo que la asignación lee y escribe; solo Room, nunca Firestore. */
interface AsignacionRepository {
    /** La incidencia tal como está en este celular, o null si no está. */
    suspend fun incidencia(id: String): Incidencia?

    fun observarIncidencia(id: String): Flow<Incidencia?>

    /** El técnico bajado del servidor (RF-619), o null si ya no es técnico. */
    suspend fun tecnico(id: String): Tecnico?

    /** Técnicos bajados del servidor, por nombre (RF-601, RF-619). */
    fun observarTecnicos(): Flow<List<Tecnico>>

    /** Oficina del equipo según el catálogo, o null si no está (RF-601). */
    suspend fun oficinaDeEquipo(codigo: String): String?

    /** RF-602: asignaciones guardadas en este celular que aún no subieron, por id de técnico. */
    fun observarAsignacionesSinSubir(): Flow<Map<String, Int>>

    suspend fun asignacionesSinSubir(tecnicoId: String): Int

    /** RF-613: guarda la incidencia y su movimiento juntos, pendientes de sincronizar. */
    suspend fun asignar(incidencia: Incidencia, movimiento: Movimiento, ahora: Instant): Resultado<Unit>
}
