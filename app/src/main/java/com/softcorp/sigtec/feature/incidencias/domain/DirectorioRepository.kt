package com.softcorp.sigtec.feature.incidencias.domain

/**
 * HU-12: la API del directorio de colaboradores (sección 14 del producto). Hoy responde un servidor de prueba en
 * MockAPI.io con la misma estructura (D-1); cambiar al servicio real es cambiar la dirección en `DirectorioModule`.
 */
interface DirectorioRepository {

    /** RF-1201: el responsable del equipo [codigoEquipo]; `NO_ENCONTRADO` si el directorio no lo tiene (RF-1211). */
    suspend fun responsableDe(codigoEquipo: String): ResultadoDirectorio
}

/** RF-1206: lo que trae el directorio. El anexo puede faltar; entonces solo se muestra el área. */
data class Colaborador(val nombre: String, val area: String, val anexo: String?)

/** El directorio es otro sistema: sus fallas no son errores del dominio de SIGTEC (`ErrorDominio`). */
sealed interface ResultadoDirectorio {
    data class Encontrado(val colaborador: Colaborador) : ResultadoDirectorio
    data class Fallo(val error: ErrorDirectorio) : ResultadoDirectorio
}

enum class ErrorDirectorio {
    NO_ENCONTRADO,   // RF-1211: el directorio no tiene un responsable para ese equipo
    SIN_RESPUESTA    // RF-1212: sin conexión, tiempo agotado, servidor caído o respuesta ilegible
}
