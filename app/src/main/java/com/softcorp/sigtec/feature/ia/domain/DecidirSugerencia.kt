package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import javax.inject.Inject

/**
 * HU-14 (RF-1413, RF-1420, RF-1421, RF-1426, plan 4.3): registra si la persona aceptó la sugerencia, con quién y
 * cuándo. La clasificación la decide quien asigna y el borrador quien atiende. Una sugerencia ya decidida no cambia.
 */
class DecidirSugerencia @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: SugerenciaIARepository
) {
    suspend operator fun invoke(sugerencia: SugerenciaIA, aceptada: Boolean, ahora: Instant): Resultado<Unit> {
        val permiso = when (sugerencia.tipo) {
            TipoSugerencia.CLASIFICACION -> Permiso.ASIGNAR_INCIDENCIA
            TipoSugerencia.BORRADOR_INFORME -> Permiso.ATENDER_INCIDENCIA
        }
        val usuario = when (val r = verificarPermiso(permiso)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        if (sugerencia.aceptada != null) return Resultado.Exito(Unit)
        return repo.decidir(sugerencia.id, aceptada, decididaPorId = usuario.id, ahora = ahora)
    }
}
