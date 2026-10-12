package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * HU-08: todo lo que la bandeja del técnico y el cierre con informe leen y escriben; solo Room,
 * nunca Firestore (D-1 del plan: repositorio nuevo de feature/atencion, igual que
 * AsignacionRepository nació aparte en la HU-06 aunque toque las mismas tablas).
 */
interface AtencionRepository {
    /** La incidencia tal como está en este celular, o null si no está (lectura fresca, D-6). */
    suspend fun incidencia(id: String): Incidencia?

    /** RF-801: solo las del técnico que no estén cerradas, de la más antigua a la más reciente. */
    fun observarBandeja(tecnicoId: String): Flow<List<Incidencia>>

    /** RF-819: guarda la incidencia Cerrada, su movimiento CERRADA y el informe en una sola transacción. */
    suspend fun cerrarConInforme(
        incidencia: Incidencia,
        movimiento: Movimiento,
        informe: InformeTecnico,
        ahora: Instant
    ): Resultado<Unit>
}
