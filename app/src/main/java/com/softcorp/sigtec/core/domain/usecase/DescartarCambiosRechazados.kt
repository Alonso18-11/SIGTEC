package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import javax.inject.Inject

/**
 * HU-05 (RF-80): «Entendido» en el aviso de rechazos. Sin `VerificarPermiso`: como el resto de la
 * sincronización, vale igual para los tres perfiles y solo limpia el celular de quien lo pulsa.
 */
class DescartarCambiosRechazados @Inject constructor(private val repo: EstadoDatosRepository) {
    suspend operator fun invoke(): Resultado<Unit> = repo.descartarRechazados()
}
