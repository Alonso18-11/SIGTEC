package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class ObservarEstadoAcceso @Inject constructor(
    private val sesion: SesionRepository,
    private val preferencias: PreferenciasSeguridadRepository,
    private val bloqueo: BloqueoApp
) {
    operator fun invoke(): Flow<EstadoAcceso> = combine(
        sesion.usuarioActual, preferencias.preferenciasHuella, bloqueo.desbloqueada
    ) { usuario, prefs, desbloqueada ->
        when {
            usuario == null -> EstadoAcceso.SinSesion
            prefs.esPropietario(usuario.id) && !desbloqueada -> EstadoAcceso.Bloqueada(usuario)
            else -> EstadoAcceso.Activa(usuario)
        }
    }.distinctUntilChanged()
}