package com.softcorp.sigtec.core.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryDemo @Inject constructor() : SesionRepository {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    override val usuarioActual: StateFlow<Usuario?> = _usuario.asStateFlow()

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> {
        val usuario = USUARIOS_DEMO.firstOrNull { it.correo == correo }
            ?: return Resultado.Error(ErrorDominio.CredencialesInvalidas)
        _usuario.value = usuario
        return Resultado.Exito(usuario)
    }

    override suspend fun cerrarSesion() {
        _usuario.value = null
    }

    companion object {
        private const val AREA = "Soporte y Sistemas"
        private const val TECNICO = "Técnico de soporte"

        val USUARIOS_DEMO = listOf(
            Usuario("demo-cmendoza", "Carlos Mendoza", "cmendoza@softcorp.pe", AREA,
                "Jefe del área de Soporte y Sistemas", Perfil.JEFE),
            Usuario("demo-lramirez", "Luis Ramírez", "lramirez@softcorp.pe", AREA, TECNICO, Perfil.TECNICO),
            Usuario("demo-atorres", "Ana Torres", "atorres@softcorp.pe", AREA, TECNICO, Perfil.TECNICO),
            Usuario("demo-jsalas", "Jorge Salas", "jsalas@softcorp.pe", AREA, TECNICO, Perfil.TECNICO),
            Usuario("demo-rvilca", "Rosa Vilca", "rvilca@softcorp.pe", AREA, TECNICO, Perfil.TECNICO),
            Usuario("demo-pcardenas", "Pedro Cárdenas", "pcardenas@softcorp.pe", AREA,
                "Analista de sistemas", Perfil.PERSONAL_SISTEMAS)
        )
    }
}