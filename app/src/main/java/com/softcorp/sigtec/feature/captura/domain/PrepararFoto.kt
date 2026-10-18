package com.softcorp.sigtec.feature.captura.domain

import java.util.UUID
import javax.inject.Inject

/**
 * HU-09 (RF-925): da el id y la ruta de la foto que la cámara va a escribir. No enlaza nada: eso lo hace
 * `GuardarEvidencia`, que verifica el permiso y la incidencia cuando la foto ya existe (P-10).
 */
class PrepararFoto @Inject constructor(
    private val repo: EvidenciaRepository
) {
    suspend operator fun invoke(incidenciaId: String): FotoNueva {
        val id = UUID.randomUUID().toString()
        return FotoNueva(id, ruta = repo.rutaParaNuevaFoto(incidenciaId, id))
    }
}
