package com.softcorp.sigtec.core.domain.usecase

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import javax.inject.Inject

/**
 * Validación de permisos en el dominio (HU-01): no depende de que el menú oculte la opción.
 * Si el usuario puede, devuelve quién es; así el caso de uso tiene sus datos en un solo paso.
 */
class VerificarPermiso @Inject constructor(private val sesion: SesionRepository) {
    operator fun invoke(permiso: Permiso): Resultado<Usuario> {
        val usuario = sesion.usuarioActual.value
            ?: return Resultado.Error(ErrorDominio.SinPermiso)
        return if (usuario.perfil.puede(permiso)) Resultado.Exito(usuario)
        else Resultado.Error(ErrorDominio.SinPermiso)
    }
}