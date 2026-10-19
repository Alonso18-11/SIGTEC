package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CerrarIncidenciaConInforme @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: AtencionRepository
) {
    suspend operator fun invoke(
        incidenciaId: String,
        diagnostico: String,
        trabajoRealizado: String,
        piezasCambiadas: String?,
        observaciones: String?,
        confirmadoEn: Instant
    ): Resultado<Incidencia> {
        val tecnico = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val incidencia = repo.incidencia(incidenciaId)
            ?: return Resultado.Error(ErrorDominio.Desconocido("incidencia no encontrada"))
        bloqueoParaCerrar(incidencia, tecnico.id)?.let { return Resultado.Error(it) }

        val campos = recortarCampos(diagnostico, trabajoRealizado, piezasCambiadas, observaciones)
        if (campos.diagnostico.isEmpty() || campos.trabajoRealizado.isEmpty()) {
            return Resultado.Error(ErrorDominio.CampoObligatorio)
        }

        val informe = InformeTecnico(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            diagnostico = campos.diagnostico,
            trabajoRealizado = campos.trabajoRealizado,
            piezasCambiadas = campos.piezasCambiadas,
            observaciones = campos.observaciones,
            fechaCierre = confirmadoEn,
            confirmadoEn = confirmadoEn
        )
        val actualizada = incidencia.copy(marca = MarcaSeguimiento.CERRADA)
        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoMovimiento.CERRADA,
            autorNombre = tecnico.nombre,
            fecha = confirmadoEn
        )
        return when (val r = repo.cerrarConInforme(actualizada, movimiento, informe, confirmadoEn)) {
            is Resultado.Exito -> Resultado.Exito(actualizada)
            is Resultado.Error -> r
        }
    }
}

fun bloqueoParaCerrar(incidencia: Incidencia, tecnicoId: String): ErrorDominio? = when {
    incidencia.tecnicoAsignadoId != tecnicoId -> ErrorDominio.SinPermiso
    incidencia.marca !in setOf(
        MarcaSeguimiento.ASIGNADA, MarcaSeguimiento.EN_ATENCION, MarcaSeguimiento.ESPERANDO_REPUESTO
    ) -> ErrorDominio.MarcaCambiada
    else -> null
}

data class CamposInforme(
    val diagnostico: String,
    val trabajoRealizado: String,
    val piezasCambiadas: String?,
    val observaciones: String?
)

fun recortarCampos(
    diagnostico: String,
    trabajoRealizado: String,
    piezasCambiadas: String?,
    observaciones: String?
): CamposInforme = CamposInforme(
    diagnostico = diagnostico.trim(),
    trabajoRealizado = trabajoRealizado.trim(),
    piezasCambiadas = piezasCambiadas?.trim()?.ifEmpty { null },
    observaciones = observaciones?.trim()?.ifEmpty { null }
)
