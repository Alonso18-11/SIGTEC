package com.softcorp.sigtec.feature.inicio.domain

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import com.softcorp.sigtec.feature.atencion.domain.SolicitudConIncidencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Inicio (specs/inicio-panel, RF-I4 a RF-I10): lo que cuenta y destaca cada perfil. */
class ResumenInicioTest {

    private val luis = Usuario("t1", "Luis Ramírez", "lramirez@softcorp.pe", "Soporte", "Técnico de soporte", Perfil.TECNICO)
    private val carlos = Usuario("j1", "Carlos Mendoza", "cmendoza@softcorp.pe", "Soporte", "Jefe", Perfil.JEFE)
    private val pedro = Usuario("p1", "Pedro Cárdenas", "pcardenas@softcorp.pe", "Sistemas", "Personal de sistemas", Perfil.PERSONAL_SISTEMAS)
    private val t0 = Instant.parse("2026-11-01T09:00:00Z")

    private fun inc(
        id: String, marca: MarcaSeguimiento, tecnico: String? = null, registro: String = "p1",
        dias: Long = 0, critica: Boolean = false
    ) = Incidencia(
        id = id, numero = null, codigoEquipo = "PC-CONT-014", descripcion = "Falla $id", usuarioResponsable = "María",
        fechaRegistro = t0.plusSeconds(dias * 86_400), registradoPorId = registro, registradoPorNombre = "Pedro",
        marca = marca, tecnicoAsignadoId = tecnico, critica = critica
    )

    private val incidencias = listOf(
        inc("a", MarcaSeguimiento.ASIGNADA, "t1"),
        inc("b", MarcaSeguimiento.ASIGNADA, "t1"),
        inc("c", MarcaSeguimiento.EN_ATENCION, "t1", dias = 3),
        inc("d", MarcaSeguimiento.EN_ATENCION, "t1", dias = 1),
        inc("e", MarcaSeguimiento.ESPERANDO_REPUESTO, "t1"),
        inc("f", MarcaSeguimiento.CERRADA, "t1"),
        inc("g", MarcaSeguimiento.EN_ATENCION, "t2", critica = true),
        inc("h", MarcaSeguimiento.SIN_ASIGNAR, registro = "j1"),
        inc("i", MarcaSeguimiento.SIN_ASIGNAR, critica = true)
    )

    private fun valores(r: ResumenInicio) = r.metricas.map { it.tipo to it.valor }

    @Test
    fun `el tecnico cuenta solo lo suyo pendiente y destaca la mas antigua en atencion`() {
        val r = calcularResumen(luis, incidencias)

        assertEquals(VistaInicio.TECNICO, r.vista)
        assertEquals(
            listOf(TipoMetrica.ASIGNADAS to 2, TipoMetrica.EN_ATENCION to 2, TipoMetrica.ESPERANDO_REPUESTO to 1),
            valores(r)
        )   // RF-I4: la cerrada y la de otro técnico no cuentan
        assertEquals("d", r.atendiendo?.id)   // RF-I7: la más antigua en atención
        assertTrue(r.puedeRegistrar)
    }

    @Test
    fun `el jefe ve todo el area`() {
        val r = calcularResumen(carlos, incidencias, ahora = t0.plusSeconds(86_400))

        assertEquals(VistaInicio.JEFE, r.vista)
        assertEquals(
            listOf(TipoMetrica.SIN_ASIGNAR to 2, TipoMetrica.EN_ATENCION to 3, TipoMetrica.CRITICAS to 2),
            valores(r)
        )   // RF-I5
        assertNull(r.atendiendo)
        assertNull(r.repuesto)
    }

    @Test
    fun `el personal de sistemas ve lo que registro`() {
        val r = calcularResumen(pedro, incidencias)

        assertEquals(VistaInicio.REGISTRADOR, r.vista)
        // RF-I6: registró a-g e i (h la registró el jefe); en atención cuenta también esperando repuesto
        assertEquals(
            listOf(TipoMetrica.PENDIENTES to 7, TipoMetrica.EN_ATENCION to 4, TipoMetrica.SOLUCIONADAS to 1),
            valores(r)
        )
    }

    @Test
    fun `sin incidencias todo es cero y no hay nada destacado`() {
        val r = calcularResumen(luis, emptyList())

        assertEquals(listOf(0, 0, 0), r.metricas.map { it.valor })   // RF-I10
        assertNull(r.atendiendo)
        assertNull(r.repuesto)
    }

    private fun solicitud(id: String, estado: EstadoEntrega, llegada: Instant? = null, numero: String? = "n$id", minutos: Long = 0) =
        SolicitudConIncidencia(
            SolicitudRepuesto(
                id = id, incidenciaId = "e", pieza = "Pieza $id", motivo = "m",
                fechaSolicitud = t0.plusSeconds(minutos * 60), confirmadoEn = t0, estadoEntrega = estado,
                solicitadoPorId = "t1", solicitadoPorNombre = "Luis", numeroLogistica = numero, llegadaEstimada = llegada
            ),
            codigoIncidencia = "INC-0116", codigoEquipo = "PC-LOG-009"
        )

    @Test
    fun `el repuesto mas proximo es el de llegada mas cercana`() {
        val r = calcularResumen(
            luis, incidencias,
            listOf(
                solicitud("tarde", EstadoEntrega.EN_CAMINO, llegada = t0.plusSeconds(5 * 86_400)),
                solicitud("pronto", EstadoEntrega.EN_CAMINO, llegada = t0.plusSeconds(86_400)),
                solicitud("sin-fecha", EstadoEntrega.POR_DESPACHAR, minutos = -10),
                solicitud("ya-llego", EstadoEntrega.ENTREGADO, llegada = t0)
            )
        )

        assertEquals(RepuestoProximo("Pieza pronto", "INC-0116", EstadoSolicitudVisible.EN_CAMINO), r.repuesto)   // RF-I8
    }

    @Test
    fun `sin llegadas estimadas el repuesto mas proximo es el mas antiguo`() {
        val proximo = repuestoProximo(
            listOf(solicitud("nueva", EstadoEntrega.POR_DESPACHAR, minutos = 30), solicitud("vieja", EstadoEntrega.POR_DESPACHAR, numero = null))
        )

        assertEquals("Pieza vieja", proximo?.pieza)
        assertEquals(EstadoSolicitudVisible.POR_ENVIAR, proximo?.estado)
    }

    @Test
    fun `entregadas y rechazadas no cuentan como proximas`() {
        val rechazada = solicitud("r", EstadoEntrega.POR_DESPACHAR).let { it.copy(solicitud = it.solicitud.copy(rechazadaPorLogistica = true)) }
        assertNull(repuestoProximo(listOf(solicitud("e", EstadoEntrega.ENTREGADO), rechazada)))
    }

    @Test
    fun `el saludo depende de la hora`() {
        assertEquals("Buenos días", saludo(7))      // RF-I1
        assertEquals("Buenas tardes", saludo(12))
        assertEquals("Buenas tardes", saludo(18))
        assertEquals("Buenas noches", saludo(19))
    }

    // HU-15 (D-1): crítica también la pendiente hace más de 5 días, como en el tablero
    @Test
    fun `para el jefe tambien es critica la pendiente hace mas de cinco dias`() {
        val r = calcularResumen(carlos, incidencias, ahora = t0.plusSeconds(10 * 86_400))
        // todas las pendientes tienen más de 5 días (las de 1 y 3 días también): 8 pendientes; la cerrada no cuenta
        assertEquals(8, r.metricas.first { it.tipo == TipoMetrica.CRITICAS }.valor)
    }
}
