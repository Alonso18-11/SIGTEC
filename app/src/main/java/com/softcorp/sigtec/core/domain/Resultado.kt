package com.softcorp.sigtec.core.domain

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Error(val causa: ErrorDominio) : Resultado<Nothing>
}

/** Errores que el usuario debe entender; cada uno se traduce a un mensaje en la pantalla. */
sealed interface ErrorDominio {
    data object SinConexion : ErrorDominio
    data object SinPermiso : ErrorDominio
    data object CredencialesInvalidas : ErrorDominio
    data class LimiteAtencionAlcanzado(val tecnicoNombre: String) : ErrorDominio
    data object ConfirmacionRequerida : ErrorDominio
    data class Desconocido(val mensaje: String?) : ErrorDominio
}