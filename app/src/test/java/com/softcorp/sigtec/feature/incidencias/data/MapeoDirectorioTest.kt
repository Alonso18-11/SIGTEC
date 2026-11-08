package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/** HU-12 (RF-1205, RF-1211, RF-1212): qué respuesta del directorio cuenta y qué fallas son «sin respuesta». */
class MapeoDirectorioTest {

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody(null)))

    private val maria = ColaboradorDto("1", "María Quispe Huamán", "Contabilidad", "2314", "PC-CONT-014")

    @Test
    fun `solo cuenta el equipo exacto aunque el servidor traiga coincidencias parciales`() {
        val respuesta = listOf(
            ColaboradorDto("2", "Otra Persona", "Logística", "2120", "PC-CONT-0145"),
            maria
        )

        assertEquals(
            listOf(Colaborador("María Quispe Huamán", "Contabilidad", "2314")),
            respuesta.coincidenciasExactas("PC-CONT-014")
        )
        assertTrue(respuesta.coincidenciasExactas("PC-CONT-01").isEmpty())   // RF-1205
    }

    @Test
    fun `no distingue mayusculas ni espacios y recorta lo que trae`() {
        val respuesta = listOf(ColaboradorDto("1", "  María Quispe Huamán ", " Contabilidad ", " 2314 ", " pc-cont-014 "))

        assertEquals(
            listOf(Colaborador("María Quispe Huamán", "Contabilidad", "2314")),
            respuesta.coincidenciasExactas("  PC-CONT-014 ")
        )
    }

    @Test
    fun `sin anexo o con anexo vacio queda null`() {
        val sinAnexo = listOf(maria.copy(anexo = null), maria.copy(id = "2", anexo = "  "))

        assertEquals(listOf(null, null), sinAnexo.coincidenciasExactas("PC-CONT-014").map { it.anexo })
    }

    @Test
    fun `un colaborador sin nombre no cuenta`() {
        assertTrue(listOf(maria.copy(nombre = "  ")).coincidenciasExactas("PC-CONT-014").isEmpty())   // RF-1211
    }

    @Test
    fun `una lista vacia no tiene responsable`() {
        assertTrue(emptyList<ColaboradorDto>().coincidenciasExactas("PC-CONT-014").isEmpty())
    }

    @Test
    fun `el 404 de MockAPI es no encontrado`() {
        assertEquals(ErrorDirectorio.NO_ENCONTRADO, errorDeDirectorio(http(404)))   // RF-1211
    }

    @Test
    fun `red, tiempo agotado, servidor caido o formato invalido son sin respuesta`() {
        // RF-1212
        listOf(IOException("sin red"), SocketTimeoutException(), http(500), http(429), SerializationException("raro"))
            .forEach { assertEquals(ErrorDirectorio.SIN_RESPUESTA, errorDeDirectorio(it)) }
    }
}
