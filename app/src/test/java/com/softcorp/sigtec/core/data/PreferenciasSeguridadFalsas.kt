package com.softcorp.sigtec.core.data

import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Doble de PreferenciasSeguridadDataStore: guarda en memoria, sin DataStore ni Android. */
class PreferenciasSeguridadFalsas : PreferenciasSeguridadRepository {
    private val estado = MutableStateFlow(PreferenciasHuella())
    override val preferenciasHuella: Flow<PreferenciasHuella> = estado
    override suspend fun asignarPropietario(id: String, nombre: String) =
        estado.update { PreferenciasHuella(id, nombre, ofrecida = true) }
    override suspend fun quitarPropietario() =
        estado.update { it.copy(propietarioId = null, propietarioNombre = null) }
    override suspend fun marcarOfrecida() = estado.update { it.copy(ofrecida = true) }
    override suspend fun limpiar() = estado.update { PreferenciasHuella() }
}
