package com.softcorp.sigtec.core.domain

import com.softcorp.sigtec.core.domain.model.Perfil

/**
 * Decisión al cerrar sesión o al dejar entrar a otra persona con cambios sin enviar.
 * `Confirmar` obliga a pedir confirmación antes de borrar los datos locales.
 */
sealed interface DecisionBorrado {
    data object SinRiesgo : DecisionBorrado
    data class Confirmar(val pendientes: Int) : DecisionBorrado
}

/** Si hay cambios sin enviar, hay que confirmar antes de borrar; si no, no hay riesgo. */ // RF-13, RF-14
fun evaluarBorrado(pendientes: Int): DecisionBorrado =
    if (pendientes > 0) DecisionBorrado.Confirmar(pendientes) else DecisionBorrado.SinRiesgo

/** Solo quita espacios de los extremos; los internos quedan intactos. */ // RF-3
fun normalizarCorreo(correo: String): String = correo.trim().lowercase()

/**
 * `null` si el texto es nulo o no coincide exactamente con un valor de `Perfil`
 * (sin ignorar mayúsculas). Un perfil inválido al ingresar bloquea el acceso (RF-7);
 * si pasa a ser inválido con la sesión abierta, fuerza el cierre (RF-15).
 */ // RF-7, RF-15
fun perfilDesdeTexto(texto: String?): Perfil? = Perfil.entries.find { it.name == texto }

/** Solo hay que borrar Room si antes había alguien autenticado y es distinto de quien entra ahora. */ // RF-11
fun debeBorrarDatosAlEntrar(uidAnterior: String?, uidNuevo: String): Boolean =
    uidAnterior != null && uidAnterior != uidNuevo

/**
 * `true` si hay un dueño bloqueado y su correo es distinto del ingresado, comparando
 * ambos ya normalizados (para no marcar como "otra persona" un simple cambio de formato).
 */ // RF-11
fun esOtraPersona(correoBloqueado: String?, correoIngresado: String): Boolean =
    correoBloqueado != null && normalizarCorreo(correoBloqueado) != normalizarCorreo(correoIngresado)
