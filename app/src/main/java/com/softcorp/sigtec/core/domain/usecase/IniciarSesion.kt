package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import javax.inject.Inject

class IniciarSesion @Inject constructor(
    private val sesion: SesionRepository,
    private val bloqueo: BloqueoApp
) {
    suspend operator fun invoke(correo: String, contrasena: String): Resultado<Usuario> {
        if (correo.isBlank()) return Resultado.Error(ErrorDominio.CredencialesInvalidas)
        val resultado = sesion.iniciarSesion(correo.trim().lowercase(), contrasena)
        if (resultado is Resultado.Exito) bloqueo.desbloquear()
        return resultado
    }
}