package com.softcorp.sigtec.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Solo se guarda a quién pertenece la huella. Ningún dato biométrico existe en la app.
@Singleton
class PreferenciasSeguridadDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferenciasSeguridadRepository {

    private val PROPIETARIO_ID = stringPreferencesKey("huella_propietario_id")
    private val PROPIETARIO_NOMBRE = stringPreferencesKey("huella_propietario_nombre")
    private val OFRECIDA = booleanPreferencesKey("huella_ofrecida")

    override val preferenciasHuella: Flow<PreferenciasHuella> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            PreferenciasHuella(
                propietarioId = it[PROPIETARIO_ID],
                propietarioNombre = it[PROPIETARIO_NOMBRE],
                ofrecida = it[OFRECIDA] ?: false
            )
        }
        .distinctUntilChanged()

    override suspend fun asignarPropietario(id: String, nombre: String) {
        dataStore.edit {
            it[PROPIETARIO_ID] = id
            it[PROPIETARIO_NOMBRE] = nombre
            it[OFRECIDA] = true
        }
    }

    override suspend fun quitarPropietario() {
        dataStore.edit {
            it.remove(PROPIETARIO_ID)
            it.remove(PROPIETARIO_NOMBRE)
        }
    }

    override suspend fun marcarOfrecida() {
        dataStore.edit { it[OFRECIDA] = true }
    }

    override suspend fun limpiar() {
        dataStore.edit { it.clear() }
    }
}