package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import com.softcorp.sigtec.feature.atencion.domain.EstadoEnLogistica
import com.softcorp.sigtec.feature.atencion.domain.PiezaLogistica
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

// HU-11 (plan 3.3): traduce lo que responde Logística al dominio, y al revés. Sin Android, se prueba en la JVM.

/** Una pieza sin nombre o con stock negativo es un dato roto de Logística y no se muestra. */
internal fun PiezaDto.aDominio(): PiezaLogistica? =
    if (nombre.isBlank() || stock < 0) null
    else PiezaLogistica(id = id, nombre = nombre.trim(), stock = stock, diasEnvio = diasEnvio.coerceAtLeast(0))

internal fun SolicitudRepuesto.aLogistica(codigoIncidencia: String, codigoEquipo: String) = SolicitudLogisticaDto(
    idSigtec = id,
    incidencia = codigoIncidencia,
    equipo = codigoEquipo,
    pieza = pieza,
    motivo = motivo,
    confirmadoEn = confirmadoEn.toString(),
    estado = estadoEntrega.name
)

/** `null` si el estado no es uno de los tres que conoce SIGTEC: se trata como respuesta ilegible (RF-1108). */
internal fun SolicitudLogisticaDto.aEstado(): EstadoEnLogistica? {
    val conocido = EstadoEntrega.entries.firstOrNull { it.name == estado.trim().uppercase() } ?: return null
    return EstadoEnLogistica(
        estado = conocido,
        despachadaEn = fecha(despachadaEn),
        entregadaEn = fecha(entregadaEn),
        llegadaEstimada = fecha(llegadaEstimada)
    )
}

// Una fecha vacía o mal escrita no impide leer el estado: solo no se muestra ese paso
private fun fecha(texto: String?): Instant? =
    texto?.takeIf { it.isNotBlank() }?.let {
        try {
            Instant.parse(it)
        } catch (e: DateTimeParseException) {
            null
        }
    }

/** RF-1108 y RF-1119: qué fallas se reintentan y cuáles no. */
internal fun errorDeLogistica(e: Throwable): ErrorLogistica = when (e) {
    is HttpException -> if (e.code() in CODIGOS_RECHAZO) ErrorLogistica.RECHAZADA else ErrorLogistica.SIN_RESPUESTA
    is IOException -> ErrorLogistica.SIN_RESPUESTA                 // sin conexión o tiempo agotado
    is SerializationException -> ErrorLogistica.SIN_RESPUESTA      // formato inesperado
    is IllegalArgumentException -> ErrorLogistica.SIN_RESPUESTA    // kotlinx.serialization también lanza esta
    else -> ErrorLogistica.SIN_RESPUESTA
}

/** MockAPI responde 404 cuando una búsqueda no encuentra nada: para SIGTEC es una lista vacía (RF-1107). */
internal fun esBusquedaSinResultados(e: Throwable): Boolean = e is HttpException && e.code() == 404

private val CODIGOS_RECHAZO = setOf(400, 404, 409, 422)
