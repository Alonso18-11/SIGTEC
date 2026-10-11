package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** HU-01: doble de prueba para controlar los cambios sin enviar (RF-11, RF-13, RF-14). */
@Singleton
class EstadoDatosRepositoryFalso @Inject constructor() : EstadoDatosRepository {

    private val _estado = MutableStateFlow(
        EstadoDatos(enLinea = true, ultimaDescarga = null, pendientesPorEnviar = 0, necesitanAtencion = 0)
    )
    override val estado: Flow<EstadoDatos> = _estado

    /** Con `true`, `descartarRechazados()` simula un fallo de Room y no cambia nada. */
    var fallar: Boolean = false

    /** Cuántas veces se pidió descartar los rechazos (RF-80). */
    var descartes: Int = 0
        private set

    /** Fija los cambios sin enviar que leerán `EvaluarCierreSesion` y `EvaluarIngresoDeOtraPersona`. */
    fun fijarPendientes(pendientes: Int) {
        _estado.value = _estado.value.copy(pendientesPorEnviar = pendientes)
    }

    /** HU-05: fija los cambios rechazados de forma permanente (RF-75, RF-79). */
    fun fijarNecesitanAtencion(n: Int) {
        _estado.value = _estado.value.copy(necesitanAtencion = n)
    }

    override suspend fun descartarRechazados(): Resultado<Unit> {
        descartes++
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _estado.value = _estado.value.copy(necesitanAtencion = 0)
        return Resultado.Exito(Unit)
    }
}
