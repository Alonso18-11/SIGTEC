package com.softcorp.sigtec.feature.atencion.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** HU-11 (plan 5.1): los dos recursos de Logística. Retrofit genera la implementación (P-1, P-2). */
interface LogisticaApi {

    // RF-1104. MockAPI responde 404 cuando nada coincide: lo traduce MapeoLogistica
    @GET("piezas")
    suspend fun buscarPiezas(@Query("search") texto: String): List<PiezaDto>

    // RF-1117
    @POST("solicitudes")
    suspend fun registrar(@Body solicitud: SolicitudLogisticaDto): SolicitudLogisticaDto

    // RF-1123
    @GET("solicitudes/{id}")
    suspend fun consultar(@Path("id") numero: String): SolicitudLogisticaDto
}

@Serializable
data class PiezaDto(
    val id: String,
    val nombre: String = "",
    val stock: Int = -1,
    val diasEnvio: Int = 0
)

@Serializable
data class SolicitudLogisticaDto(
    val id: String? = null,          // lo pone Logística al registrarla
    val idSigtec: String,
    val incidencia: String,
    val equipo: String,
    val pieza: String,
    val motivo: String,
    val confirmadoEn: String,        // ISO-8601, como todas las fechas
    val estado: String,
    val despachadaEn: String? = null,
    val entregadaEn: String? = null,
    val llegadaEstimada: String? = null
)
