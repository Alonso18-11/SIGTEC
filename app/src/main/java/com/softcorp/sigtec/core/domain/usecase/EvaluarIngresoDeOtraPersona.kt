package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.domain.esOtraPersona
import com.softcorp.sigtec.core.domain.evaluarBorrado
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Si hay un dueño de huella bloqueado y el correo ingresado es de otra persona, hay que
 * confirmar antes de borrar sus datos sin enviar; si es el mismo dueño o no hay nadie
 * bloqueado, no hay riesgo. No borra nada: solo decide. // RF-11, RF-13
 */
class EvaluarIngresoDeOtraPersona @Inject constructor(
    private val observarEstadoAcceso: ObservarEstadoAcceso,
    private val observarEstadoDatos: ObservarEstadoDatos
) {
    suspend operator fun invoke(correoIngresado: String): DecisionBorrado {
        val correoBloqueado = (observarEstadoAcceso().first() as? EstadoAcceso.Bloqueada)?.usuario?.correo
        if (!esOtraPersona(correoBloqueado, correoIngresado)) return DecisionBorrado.SinRiesgo
        val pendientes = observarEstadoDatos().first().pendientesPorEnviar
        return evaluarBorrado(pendientes)
    }
}
