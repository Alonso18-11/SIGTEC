package com.softcorp.sigtec.core.domain.model

data class Usuario(
    val id: String,          // el mismo uid de Firebase Authentication
    val nombre: String,
    val correo: String,
    val area: String,
    val cargo: String,
    val perfil: Perfil
)