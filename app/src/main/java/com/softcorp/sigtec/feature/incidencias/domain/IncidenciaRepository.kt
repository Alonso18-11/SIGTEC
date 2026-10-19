package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface IncidenciaRepository {
    /** Guarda la incidencia y su primer movimiento juntos, pendientes de sincronizar (HU-03). */
    suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit>

    /** Todas, de la más reciente a la más antigua (HU-04). */
    fun observarTodas(): Flow<List<Incidencia>>

    fun observarPorId(id: String): Flow<Incidencia?>

    /** Línea de tiempo de la incidencia, del movimiento más reciente al más antiguo. */
    fun observarMovimientos(incidenciaId: String): Flow<List<Movimiento>>

    // RF-41
    /** True si el código ya existe en el catálogo de equipos sincronizado. */
    suspend fun existeEquipo(codigo: String): Boolean

    // HU-08: lectura fresca para revalidar la marca justo antes de guardar (RF-807, RF-808)
    suspend fun porId(id: String): Incidencia?

    /** Guarda el cambio a En atención y su movimiento ATENCION_INICIADA en una sola transacción (RF-809). */
    suspend fun iniciarAtencion(incidencia: Incidencia, movimiento: Movimiento, ahora: Instant): Resultado<Unit>

    /** El informe técnico de la incidencia, si ya existe (RF-827, RF-828). */
    fun observarInforme(incidenciaId: String): Flow<InformeTecnico?>

    /** HU-09 (RF-932, RF-936): sus fotos en este celular, de la más antigua a la más reciente. Solo lectura. */
    fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>>
}
