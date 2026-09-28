package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CerrarSesion @Inject constructor(
    private val sesion: SesionRepository,
    private val preferencias: PreferenciasSeguridadRepository,
    private val bloqueo: BloqueoApp
) {
    suspend operator fun invoke() {
        val usuario = sesion.usuarioActual.value
        val prefs = preferencias.preferenciasHuella.first()

        // Si se va el dueño, el celular deja de estar vinculado a él.
        // Si se va otra persona, la configuración del dueño se respeta.
        if (usuario != null && prefs.esPropietario(usuario.id)) preferencias.limpiar()

        bloqueo.bloquear()
        sesion.cerrarSesion()
    }
}