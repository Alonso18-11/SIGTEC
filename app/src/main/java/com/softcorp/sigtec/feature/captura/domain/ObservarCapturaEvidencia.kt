package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Lo que muestra «Evidencia fotográfica»: la incidencia, sus fotos y, si no se pueden agregar más, por qué. */
data class CapturaEvidencia(
    val incidencia: Incidencia,
    val fotos: List<Evidencia>,
    val bloqueo: ErrorDominio?
)

/**
 * HU-09 (RF-924, RF-929, RF-931): observa la incidencia y sus fotos en Room junto con la sesión. Si la incidencia
 * se cierra o se reasigna desde otro celular, o llega a 10 fotos, el bloqueo aparece solo y la pantalla deshabilita
 * el disparador. Emite `null` si la incidencia no está en este celular.
 */
class ObservarCapturaEvidencia @Inject constructor(
    private val repo: EvidenciaRepository,
    private val observarUsuarioActual: ObservarUsuarioActual
) {
    operator fun invoke(incidenciaId: String): Flow<CapturaEvidencia?> = combine(
        repo.observarIncidencia(incidenciaId),
        repo.observarEvidencias(incidenciaId),
        observarUsuarioActual()
    ) { incidencia, fotos, usuario ->
        incidencia?.let { CapturaEvidencia(it, fotos, bloqueo = bloqueoParaFotos(usuario, it, fotos.size)) }
    }
}
