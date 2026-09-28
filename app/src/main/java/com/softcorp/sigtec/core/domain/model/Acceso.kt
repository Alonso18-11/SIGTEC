package com.softcorp.sigtec.core.domain.model

sealed interface EstadoAcceso {
    data object SinSesion : EstadoAcceso
    data class Bloqueada(val usuario: Usuario) : EstadoAcceso
    data class Activa(val usuario: Usuario) : EstadoAcceso
}

data class PreferenciasHuella(
    val propietarioId: String? = null,
    val propietarioNombre: String? = null,
    val ofrecida: Boolean = false
) {
    val activada: Boolean get() = propietarioId != null
    fun esPropietario(usuarioId: String): Boolean = propietarioId == usuarioId
}