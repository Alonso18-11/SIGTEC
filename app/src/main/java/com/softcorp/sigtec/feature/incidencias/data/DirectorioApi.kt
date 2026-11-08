package com.softcorp.sigtec.feature.incidencias.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** HU-12 (plan 3.2): el recurso `colaboradores` del directorio. Retrofit genera la implementación. */
interface DirectorioApi {

    // RF-1201. MockAPI filtra por coincidencia parcial y responde 404 si nada coincide: lo resuelve MapeoDirectorio
    @GET("colaboradores")
    suspend fun buscarPorEquipo(@Query("equipo") codigoEquipo: String): List<ColaboradorDto>
}

@Serializable
data class ColaboradorDto(
    val id: String,
    val nombre: String = "",
    val area: String = "",
    val anexo: String? = null,
    val equipo: String = ""
)
