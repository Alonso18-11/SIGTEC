package com.softcorp.sigtec.core.domain

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Error(val causa: ErrorDominio) : Resultado<Nothing>
}

/** Errores que el usuario debe entender; cada uno se traduce a un mensaje en MensajesError.kt. */
sealed interface ErrorDominio {
    data object SinConexion : ErrorDominio
    data object SinPermiso : ErrorDominio
    data object CredencialesInvalidas : ErrorDominio
    data object DemasiadosIntentos : ErrorDominio
    data object PerfilNoConfigurado : ErrorDominio
    data object CampoObligatorio : ErrorDominio          // falta un dato requerido del formulario
    data class LimiteAtencionAlcanzado(val tecnicoNombre: String) : ErrorDominio
    data object ConfirmacionRequerida : ErrorDominio
    data object EquipoNoEncontrado : ErrorDominio        // RF-41
    data object IncidenciaCerrada : ErrorDominio         // RF-609: ya no se puede asignar
    data object IncidenciaPorSincronizar : ErrorDominio  // RF-610: sin número, aún no está en el servidor
    data class YaAsignada(val tecnicoNombre: String) : ErrorDominio   // RF-611
    data class Desconocido(val mensaje: String?) : ErrorDominio
}