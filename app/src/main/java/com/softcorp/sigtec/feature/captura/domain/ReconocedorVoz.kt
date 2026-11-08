package com.softcorp.sigtec.feature.captura.domain

import kotlinx.coroutines.flow.Flow

/**
 * HU-10: el reconocimiento de voz del propio celular (RF-1006, D-1). Nunca guarda el audio (RF-1013).
 * Una escucha por llamada a [escuchar] (P-2).
 */
interface ReconocedorVoz {

    /** Falso si el celular no tiene un servicio de reconocimiento de voz (RF-1017). */
    val disponible: Boolean

    /**
     * Escucha hasta que la persona deja de hablar, se llama a [detener] o hay un error. El flujo es frío: termina
     * con [EventoDictado.Final] o [EventoDictado.Error], y dejar de recolectarlo suelta el micrófono (RF-1012, RF-1014).
     */
    fun escuchar(): Flow<EventoDictado>

    /** Pide terminar la escucha en curso; lo reconocido llega en [EventoDictado.Final] (RF-1007). */
    fun detener()
}

sealed interface EventoDictado {
    /** El micrófono ya está escuchando (RF-1004). */
    data object Escuchando : EventoDictado

    /** Lo reconocido hasta ahora, mientras la persona habla (RF-1005). */
    data class Parcial(val texto: String) : EventoDictado

    /** Lo reconocido al terminar la escucha; puede venir vacío (RF-1007). */
    data class Final(val texto: String) : EventoDictado

    data class Error(val error: ErrorDictado) : EventoDictado
}

/** Por qué se detuvo una escucha sin resultado (RF-1015 a RF-1018). */
enum class ErrorDictado {
    SIN_ENTENDER,   // RF-1015: silencio, ruido o habla poco clara
    SIN_CONEXION,   // RF-1016: sin red y sin reconocimiento sin conexión
    SIN_SERVICIO,   // RF-1017: el celular no tiene reconocimiento de voz
    SIN_PERMISO,    // el permiso se quitó mientras escuchaba: vuelve a la explicación (RF-1021)
    MICROFONO       // RF-1018: micrófono ocupado o cualquier otra falla
}
