package com.softcorp.sigtec.core.domain.repository

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Usuario
import kotlinx.coroutines.flow.StateFlow

interface SesionRepository {

    /** null = no hay sesión. Emite cada vez que alguien entra o sale. */
    val usuarioActual: StateFlow<Usuario?>

    suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario>

    suspend fun cerrarSesion()
}