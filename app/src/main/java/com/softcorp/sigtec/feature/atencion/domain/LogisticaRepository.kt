package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import java.time.Instant

/**
 * HU-11: la API de Logística (sección 14 del producto). Hoy responde un servidor de prueba en MockAPI.io con la
 * misma estructura (D-1); cambiar al servicio real es cambiar la dirección en `LogisticaModule`.
 */
interface LogisticaRepository {

    /** RF-1104: las piezas del catálogo cuyo nombre contiene [texto]; lista vacía si ninguna coincide (RF-1107). */
    suspend fun buscarPiezas(texto: String): ResultadoLogistica<List<PiezaLogistica>>

    /** RF-1117: registra la solicitud en Logística y devuelve su número. */
    suspend fun registrar(
        solicitud: SolicitudRepuesto,
        codigoIncidencia: String,
        codigoEquipo: String
    ): ResultadoLogistica<String>

    /** RF-1123: el estado actual de una solicitud ya registrada. */
    suspend fun consultar(numero: String): ResultadoLogistica<EstadoEnLogistica>
}

data class PiezaLogistica(val id: String, val nombre: String, val stock: Int, val diasEnvio: Int)

data class EstadoEnLogistica(
    val estado: EstadoEntrega,
    val despachadaEn: Instant?,
    val entregadaEn: Instant?,
    val llegadaEstimada: Instant?
)

/** Logística es otro sistema: sus fallas no son errores del dominio de SIGTEC (`ErrorDominio`). */
sealed interface ResultadoLogistica<out T> {
    data class Exito<T>(val valor: T) : ResultadoLogistica<T>
    data class Fallo(val error: ErrorLogistica) : ResultadoLogistica<Nothing>
}

enum class ErrorLogistica {
    SIN_RESPUESTA,   // RF-1108: sin conexión, tiempo agotado, servidor caído o respuesta ilegible; se reintenta
    RECHAZADA        // RF-1119: Logística no acepta los datos; reintentar no sirve
}

/** Para no repetir el `when` en los casos de uso: el valor o `null`, y el error aparte. */
fun <T> ResultadoLogistica<T>.valorONulo(): T? = (this as? ResultadoLogistica.Exito)?.valor

fun ResultadoLogistica<*>.errorONulo(): ErrorLogistica? = (this as? ResultadoLogistica.Fallo)?.error
