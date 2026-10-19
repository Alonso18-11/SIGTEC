package com.softcorp.sigtec.core.domain

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Error(val causa: ErrorDominio) : Resultado<Nothing>
}

sealed interface ErrorDominio {
    data object SinConexion : ErrorDominio
    data object SinPermiso : ErrorDominio
    data object CredencialesInvalidas : ErrorDominio
    data object DemasiadosIntentos : ErrorDominio
    data object PerfilNoConfigurado : ErrorDominio
    data object CampoObligatorio : ErrorDominio          // falta un dato requerido del formulario
    data class LimiteAtencionAlcanzado(val tecnicoNombre: String) : ErrorDominio
    data object ConfirmacionRequerida : ErrorDominio
    data object EquipoNoEncontrado : ErrorDominio
    data object IncidenciaCerrada : ErrorDominio
    data object IncidenciaPorSincronizar : ErrorDominio  // RF-610: sin número, aún no está en el servidor
    data class YaAsignada(val tecnicoNombre: String) : ErrorDominio   // RF-611
    data object MarcaCambiada : ErrorDominio             // RF-808: la marca cambió por otra vía antes de guardar
    data object LimiteFotosAlcanzado : ErrorDominio      // RF-929: ya hay 10 fotos de la incidencia en el celular
    data class Desconocido(val mensaje: String?) : ErrorDominio
}