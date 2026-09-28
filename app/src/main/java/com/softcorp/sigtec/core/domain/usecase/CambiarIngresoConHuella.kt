package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CambiarIngresoConHuella @Inject constructor(
    private val sesion: SesionRepository,
    private val preferencias: PreferenciasSeguridadRepository,
    private val bloqueo: BloqueoApp
) {
    suspend operator fun invoke(activar: Boolean): Resultado<Unit> {
        val usuario = sesion.usuarioActual.value
            ?: return Resultado.Error(ErrorDominio.SinPermiso)
        val prefs = preferencias.preferenciasHuella.first()

        // La huella del celular es de una sola persona: nadie más la toma ni la quita
        if (prefs.activada && !prefs.esPropietario(usuario.id)) {
            return Resultado.Error(ErrorDominio.SinPermiso)
        }

        if (activar) {
            // Quien la activa acaba de verificarse: no se le bloquea en ese instante
            bloqueo.desbloquear()
            preferencias.asignarPropietario(usuario.id, usuario.nombre)
        } else {
            preferencias.quitarPropietario()
        }
        return Resultado.Exito(Unit)
    }
}