package com.softcorp.sigtec.core.domain.repository

import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import kotlinx.coroutines.flow.Flow

interface PreferenciasSeguridadRepository {
    val preferenciasHuella: Flow<PreferenciasHuella>

    /** Vincula la huella a esta persona y marca la oferta como hecha. */
    suspend fun asignarPropietario(id: String, nombre: String)

    /** Desvincula la huella; la oferta sigue marcada como hecha. */
    suspend fun quitarPropietario()

    suspend fun marcarOfrecida()

    suspend fun limpiar()
}