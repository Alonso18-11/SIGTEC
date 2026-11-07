package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import javax.inject.Inject

/**
 * HU-09 (RF-937, D-5): elimina una foto y su archivo. Solo el técnico asignado y mientras la incidencia siga
 * pendiente; con la incidencia cerrada, la foto queda como respaldo del informe. El máximo de fotos no aplica.
 */
class EliminarEvidencia @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: EvidenciaRepository
) {
    suspend operator fun invoke(evidencia: Evidencia): Resultado<Unit> {
        val usuario = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        // Lectura fresca: la incidencia pudo cerrarse o reasignarse con la foto abierta
        val incidencia = repo.incidencia(evidencia.incidenciaId)
        bloqueoParaFotos(usuario, incidencia, cantidad = null)?.let { return Resultado.Error(it) }
        return repo.eliminar(evidencia)
    }
}
