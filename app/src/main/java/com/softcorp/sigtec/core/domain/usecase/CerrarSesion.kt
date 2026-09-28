package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.repository.SesionRepository
import javax.inject.Inject

class CerrarSesion @Inject constructor(private val sesion: SesionRepository) {
    suspend operator fun invoke() = sesion.cerrarSesion()
}