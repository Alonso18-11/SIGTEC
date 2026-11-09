package com.softcorp.sigtec.feature.inicio.domain

import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Inicio (RF-I11, RF-I12): el resumen de la persona con sesión, leído de Room y actualizado solo cuando cambian los
 * datos. Sin sesión, `null`. Reutiliza los repositorios de incidencias y de atención; no agrega consultas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservarResumenInicio @Inject constructor(
    private val observarUsuarioActual: ObservarUsuarioActual,
    private val incidencias: IncidenciaRepository,
    private val atencion: AtencionRepository
) {
    operator fun invoke(): Flow<ResumenInicio?> = observarUsuarioActual().flatMapLatest { usuario ->
        when {
            usuario == null -> flowOf(null)
            vistaPara(usuario) == VistaInicio.TECNICO ->
                combine(incidencias.observarTodas(), atencion.observarSolicitudesDe(usuario.id)) { lista, solicitudes ->
                    calcularResumen(usuario, lista, solicitudes)
                }
            else -> incidencias.observarTodas().map { calcularResumen(usuario, it) }
        }
    }
}
