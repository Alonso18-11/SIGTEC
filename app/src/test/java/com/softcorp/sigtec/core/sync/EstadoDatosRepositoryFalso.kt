package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** HU-01: doble de prueba para controlar los cambios sin enviar (RF-11, RF-13, RF-14). */
@Singleton
class EstadoDatosRepositoryFalso @Inject constructor() : EstadoDatosRepository {

    private val _estado = MutableStateFlow(EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 0))
    override val estado: Flow<EstadoDatos> = _estado

    /** Fija los cambios sin enviar que leerán `EvaluarCierreSesion` y `EvaluarIngresoDeOtraPersona`. */
    fun fijarPendientes(pendientes: Int) {
        _estado.value = _estado.value.copy(pendientesPorEnviar = pendientes)
    }
}
