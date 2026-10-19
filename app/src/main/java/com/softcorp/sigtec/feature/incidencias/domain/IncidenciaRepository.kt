package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface IncidenciaRepository {
    suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit>

    fun observarTodas(): Flow<List<Incidencia>>

    fun observarPorId(id: String): Flow<Incidencia?>

    fun observarMovimientos(incidenciaId: String): Flow<List<Movimiento>>

    suspend fun existeEquipo(codigo: String): Boolean

    suspend fun porId(id: String): Incidencia?

    suspend fun iniciarAtencion(incidencia: Incidencia, movimiento: Movimiento, ahora: Instant): Resultado<Unit>

    fun observarInforme(incidenciaId: String): Flow<InformeTecnico?>

    fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>>
}
