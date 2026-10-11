package com.softcorp.sigtec.core.domain.repository

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoDatos
import kotlinx.coroutines.flow.Flow

interface EstadoDatosRepository {
    val estado: Flow<EstadoDatos>

    /** RF-80 (HU-05): descarta del celular lo que el servidor rechazó; queda su versión. */
    suspend fun descartarRechazados(): Resultado<Unit>
}
