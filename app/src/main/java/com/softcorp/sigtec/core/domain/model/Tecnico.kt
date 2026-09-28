package com.softcorp.sigtec.core.domain.model

data class Tecnico(
    val id: String,
    val nombre: String,
    val limiteAtencion: Int,
    val incidenciasActivas: Int
) {
    // Se calcula, no se guarda: nunca puede contradecir a la carga real.
    val disponible: Boolean
        get() = incidenciasActivas < limiteAtencion

    // Para la barra de carga de la pantalla de asignación (0.0 a 1.0)
    val cargaRelativa: Float
        get() = (incidenciasActivas.toFloat() / limiteAtencion).coerceIn(0f, 1f)
}