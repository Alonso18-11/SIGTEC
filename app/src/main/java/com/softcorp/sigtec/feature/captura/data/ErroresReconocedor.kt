package com.softcorp.sigtec.feature.captura.data

import android.speech.SpeechRecognizer
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado

/**
 * HU-10 (plan 3.3): traduce el código de error de `SpeechRecognizer` al error del dominio. Las constantes son de
 * compilación, así que la prueba corre en la JVM sin celular.
 */
fun errorDictadoDe(codigo: Int): ErrorDictado = when (codigo) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ErrorDictado.SIN_ENTENDER                  // RF-1015

    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER,
    ERROR_SERVER_DISCONNECTED,
    ERROR_LANGUAGE_UNAVAILABLE -> ErrorDictado.SIN_CONEXION                              // RF-1016

    // El reconocedor ya probó todos los españoles (ReconocedorVozAndroid): el servicio de voz no tiene ninguno
    ERROR_LANGUAGE_NOT_SUPPORTED -> ErrorDictado.SIN_SERVICIO                             // RF-1017

    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ErrorDictado.SIN_PERMISO         // RF-1021

    else -> ErrorDictado.MICROFONO                                                       // RF-1018
}

// Existen desde Android 12 (API 31); minSdk es 26, así que se escriben con su valor para no exigir esa versión
private const val ERROR_SERVER_DISCONNECTED = 11
private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
private const val ERROR_LANGUAGE_UNAVAILABLE = 13
