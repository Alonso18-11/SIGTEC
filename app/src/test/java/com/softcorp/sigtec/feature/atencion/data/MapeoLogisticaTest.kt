package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant

/** HU-11 (RF-1105 a RF-1108, RF-1117, RF-1119, RF-1123): lo que responde Logística y cómo se le envía. */
class MapeoLogisticaTest {

    private val confirmadoEn = Instant.parse("2026-11-07T15:30:00Z")

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody(null)))

    @Test
    fun `una pieza valida pasa al dominio sin espacios en el nombre`() {
        val pieza = PiezaDto("1", "  Fuente de poder ATX 500 W ", stock = 3, diasEnvio = 1).aDominio()!!
        assertEquals("Fuente de poder ATX 500 W", pieza.nombre)
        assertEquals(3, pieza.stock)
        assertEquals(1, pieza.diasEnvio)
    }

    @Test
    fun `una pieza sin nombre o con stock negativo se descarta`() {
        assertNull(PiezaDto("1", "", stock = 3).aDominio())
        assertNull(PiezaDto("2", "Teclado USB", stock = -1).aDominio())
    }

    @Test
    fun `stock cero es una pieza valida sin stock`() {
        assertEquals(0, PiezaDto("6", "Mouse óptico USB", stock = 0, diasEnvio = 3).aDominio()!!.stock)   // RF-1105
    }

    @Test
    fun `la solicitud se envia con el codigo visible, el equipo y la fecha en ISO`() {
        val dto = solicitud().aLogistica(codigoIncidencia = "INC-0124", codigoEquipo = "PC-CONT-014")

        assertNull(dto.id)
        assertEquals("s1", dto.idSigtec)
        assertEquals("INC-0124", dto.incidencia)
        assertEquals("PC-CONT-014", dto.equipo)
        assertEquals("2026-11-07T15:30:00Z", dto.confirmadoEn)
        assertEquals("POR_DESPACHAR", dto.estado)
    }

    @Test
    fun `el estado de Logistica se lee con sus fechas`() {
        val estado = dto(estado = "en_camino", despachadaEn = "2026-11-08T10:00:00Z", llegadaEstimada = "2026-11-09T10:00:00Z").aEstado()!!

        assertEquals(EstadoEntrega.EN_CAMINO, estado.estado)   // RF-1123
        assertEquals(Instant.parse("2026-11-08T10:00:00Z"), estado.despachadaEn)
        assertEquals(Instant.parse("2026-11-09T10:00:00Z"), estado.llegadaEstimada)
        assertNull(estado.entregadaEn)
    }

    @Test
    fun `un estado desconocido es una respuesta ilegible`() {
        assertNull(dto(estado = "PERDIDO").aEstado())
    }

    @Test
    fun `una fecha mal escrita no impide leer el estado`() {
        val estado = dto(estado = "ENTREGADO", entregadaEn = "mañana").aEstado()!!
        assertEquals(EstadoEntrega.ENTREGADO, estado.estado)
        assertNull(estado.entregadaEn)
    }

    @Test
    fun `sin conexion, tiempo agotado o servidor caido se reintenta`() {
        assertEquals(ErrorLogistica.SIN_RESPUESTA, errorDeLogistica(IOException("sin red")))   // RF-1108
        assertEquals(ErrorLogistica.SIN_RESPUESTA, errorDeLogistica(SocketTimeoutException()))
        assertEquals(ErrorLogistica.SIN_RESPUESTA, errorDeLogistica(http(500)))
        assertEquals(ErrorLogistica.SIN_RESPUESTA, errorDeLogistica(http(429)))
        assertEquals(ErrorLogistica.SIN_RESPUESTA, errorDeLogistica(SerializationException("formato")))
    }

    @Test
    fun `los datos que Logistica no acepta no se reintentan`() {
        assertEquals(ErrorLogistica.RECHAZADA, errorDeLogistica(http(400)))   // RF-1119
        assertEquals(ErrorLogistica.RECHAZADA, errorDeLogistica(http(422)))
    }

    @Test
    fun `un 404 en la busqueda es que no hay coincidencias`() {
        assertTrue(esBusquedaSinResultados(http(404)))   // RF-1107
        assertFalse(esBusquedaSinResultados(http(500)))
        assertFalse(esBusquedaSinResultados(IOException()))
    }

    private fun solicitud() = SolicitudRepuesto(
        id = "s1", incidenciaId = "i124", pieza = "Fuente de poder ATX 500 W", motivo = "No entrega energía",
        fechaSolicitud = confirmadoEn, confirmadoEn = confirmadoEn, estadoEntrega = EstadoEntrega.POR_DESPACHAR,
        solicitadoPorId = "demo-lramirez", solicitadoPorNombre = "Luis Ramírez"
    )

    private fun dto(
        estado: String,
        despachadaEn: String? = null,
        entregadaEn: String? = null,
        llegadaEstimada: String? = null
    ) = SolicitudLogisticaDto(
        id = "17", idSigtec = "s1", incidencia = "INC-0124", equipo = "PC-CONT-014",
        pieza = "Fuente de poder ATX 500 W", motivo = "No entrega energía", confirmadoEn = "2026-11-07T15:30:00Z",
        estado = estado, despachadaEn = despachadaEn, entregadaEn = entregadaEn, llegadaEstimada = llegadaEstimada
    )
}
