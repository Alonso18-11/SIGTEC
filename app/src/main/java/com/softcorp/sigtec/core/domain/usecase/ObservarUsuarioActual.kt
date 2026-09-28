package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObservarUsuarioActual @Inject constructor(private val sesion: SesionRepository) {
    operator fun invoke(): StateFlow<Usuario?> = sesion.usuarioActual
}