package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.domain.evaluarBorrado
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Decide si cerrar sesión requiere confirmación; no cierra nada. */ // RF-13, RF-14
class EvaluarCierreSesion @Inject constructor(
    private val observarEstadoDatos: ObservarEstadoDatos
) {
    suspend operator fun invoke(): DecisionBorrado {
        val estado = observarEstadoDatos().first()
        return evaluarBorrado(estado.pendientesPorEnviar)
    }
}
