package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Cuántas cambiaron y cuántas no se pudieron consultar (sin conexión o Logística sin responder). */
data class ResultadoActualizacion(val actualizadas: Int, val sinRespuesta: Int)

/**
 * HU-11 (plan 4.5, RF-1123, RF-1124): consulta a Logística el estado de las solicitudes del técnico que todavía no se
 * entregaron y guarda lo que cambió. Con la entrega del último repuesto de una incidencia, la deja otra vez en
 * «En atención» (D-7). Una consulta que falla no frena las demás. La HU-18 llamará a lo mismo una vez al día.
 */
class ActualizarEstadoRepuestos @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: AtencionRepository,
    private val logistica: LogisticaRepository
) {
    suspend operator fun invoke(ahora: Instant): Resultado<ResultadoActualizacion> {
        val tecnico = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        var actualizadas = 0
        var sinRespuesta = 0
        for (solicitud in repo.solicitudesSinEntregarDe(tecnico.id)) {
            val numero = solicitud.numeroLogistica ?: continue
            val remoto = when (val r = logistica.consultar(numero)) {
                is ResultadoLogistica.Exito -> r.valor
                is ResultadoLogistica.Fallo -> { sinRespuesta++; continue }
            }
            val nueva = solicitud.conEstado(remoto)
            if (nueva == solicitud) continue   // nada cambió

            var incidencia: Incidencia? = null
            var movimiento: Movimiento? = null
            if (nueva.estadoEntrega == EstadoEntrega.ENTREGADO) {
                val actual = repo.incidencia(solicitud.incidenciaId)
                // Esta solicitud todavía cuenta como sin entregar en Room: las que quedan son las demás
                val quedan = repo.contarSinEntregarDe(solicitud.incidenciaId) - 1
                val marca = actual?.let { marcaAlEntregar(it.marca, quedan) }   // RF-1124
                if (actual != null && marca != null) {
                    incidencia = actual.copy(marca = marca)
                    movimiento = Movimiento(
                        id = UUID.randomUUID().toString(),
                        incidenciaId = actual.id,
                        tipo = TipoMovimiento.REPUESTO_RECIBIDO,
                        autorNombre = "Logística",
                        fecha = nueva.entregadaEn ?: ahora,
                        detalle = nueva.pieza
                    )
                }
            }
            when (repo.aplicarEstado(nueva, incidencia, movimiento, ahora)) {
                is Resultado.Exito -> actualizadas++
                is Resultado.Error -> sinRespuesta++
            }
        }
        repo.registrarConsultaLogistica(ahora, exitosa = sinRespuesta == 0)
        return Resultado.Exito(ResultadoActualizacion(actualizadas, sinRespuesta))
    }
}

/** Lo que informa Logística reemplaza el estado; una fecha que no informa no borra la que ya se sabía. */
private fun SolicitudRepuesto.conEstado(remoto: EstadoEnLogistica) = copy(
    estadoEntrega = remoto.estado,
    despachadaEn = remoto.despachadaEn ?: despachadaEn,
    entregadaEn = remoto.entregadaEn ?: entregadaEn,
    llegadaEstimada = remoto.llegadaEstimada ?: llegadaEstimada
)
