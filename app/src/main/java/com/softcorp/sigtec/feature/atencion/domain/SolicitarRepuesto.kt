package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Lo que el técnico vio de la pieza antes de pedirla (RF-1113); `null` si no se pudo consultar (D-2). */
data class PiezaConsultada(val stock: Int, val diasEnvio: Int)

/**
 * HU-11 (plan 4.1): guarda la solicitud de repuesto confirmada con huella. Solo escribe en Room; registrarla en
 * Logística y subirla a Firestore se hace después, en segundo plano (RF-1117, RF-1118). La fecha de la solicitud y
 * [confirmadoEn] las decide quien llama, nunca un `now()` aquí (P-5).
 */
class SolicitarRepuesto @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: AtencionRepository
) {
    suspend operator fun invoke(
        incidenciaId: String,
        pieza: String,
        motivo: String,
        consultada: PiezaConsultada?,
        confirmadoEn: Instant,
        ahora: Instant
    ): Resultado<SolicitudRepuesto> {
        val tecnico = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val incidencia = repo.incidencia(incidenciaId)
            ?: return Resultado.Error(ErrorDominio.Desconocido("incidencia no encontrada"))
        bloqueoParaSolicitar(tecnico, incidencia)?.let { return Resultado.Error(it) }   // RF-1102

        val piezaLimpia = pieza.trim()
        val motivoLimpio = motivo.trim()
        if (piezaLimpia.isEmpty() || motivoLimpio.isEmpty()) return Resultado.Error(ErrorDominio.CampoObligatorio)   // RF-1110

        val solicitud = SolicitudRepuesto(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            pieza = piezaLimpia,
            motivo = motivoLimpio,
            fechaSolicitud = ahora,
            confirmadoEn = confirmadoEn,          // RF-23 del producto: sin huella no hay solicitud
            estadoEntrega = EstadoEntrega.POR_DESPACHAR,
            solicitadoPorId = tecnico.id,
            solicitadoPorNombre = tecnico.nombre,
            stockConsultado = consultada?.stock,
            diasEnvio = consultada?.diasEnvio
        )
        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoMovimiento.REPUESTO_SOLICITADO,
            autorNombre = tecnico.nombre,
            fecha = confirmadoEn,
            detalle = piezaLimpia
        )
        val actualizada = incidencia.copy(marca = marcaAlSolicitar(incidencia.marca))   // RF-1113
        return when (val r = repo.solicitarRepuesto(actualizada, movimiento, solicitud, ahora)) {
            is Resultado.Exito -> Resultado.Exito(solicitud)
            is Resultado.Error -> r                                                       // RF-1115
        }
    }
}
