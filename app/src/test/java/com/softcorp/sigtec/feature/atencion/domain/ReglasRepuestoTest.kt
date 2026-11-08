package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1102, RF-1105, RF-1113, RF-1120, RF-1122, RF-1124): quién pide, qué se ve y cuándo se retoma. */
class ReglasRepuestoTest {

    private val t0 = Instant.parse("2026-11-03T10:00:00Z")

    private val luis = Usuario("demo-lramirez", "Luis Ramírez", "lramirez@softcorp.pe", "Soporte", "Técnico", Perfil.TECNICO)
    private val ana = luis.copy(id = "demo-atorres", nombre = "Ana Torres")
    private val jefe = luis.copy(id = "demo-cmendoza", nombre = "Carlos Mendoza", perfil = Perfil.JEFE)

    private fun incidencia(marca: MarcaSeguimiento, tecnicoId: String? = "demo-lramirez") = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = t0, registradoPorId = "demo-pcardenas",
        registradoPorNombre = "Pedro Cárdenas", marca = marca, tecnicoAsignadoId = tecnicoId,
        tecnicoAsignadoNombre = tecnicoId?.let { "Luis Ramírez" }
    )

    private fun solicitud(
        numero: String? = "17",
        estado: EstadoEntrega = EstadoEntrega.POR_DESPACHAR,
        rechazada: Boolean = false,
        entregadaEn: Instant? = null
    ) = SolicitudRepuesto(
        id = "s1", incidenciaId = "i124", pieza = "Fuente de poder ATX 500 W", motivo = "No entrega energía",
        fechaSolicitud = t0, confirmadoEn = t0, estadoEntrega = estado, solicitadoPorId = "demo-lramirez",
        solicitadoPorNombre = "Luis Ramírez", numeroLogistica = numero, entregadaEn = entregadaEn,
        rechazadaPorLogistica = rechazada
    )

    @Test
    fun `el tecnico asignado solicita en asignada, en atencion o esperando repuesto`() {
        MARCAS_PARA_SOLICITAR.forEach { marca ->
            assertNull(marca.name, bloqueoParaSolicitar(luis, incidencia(marca)))   // RF-1102, D-5
        }
    }

    @Test
    fun `otro tecnico o el jefe no solicitan`() {
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaSolicitar(ana, incidencia(MarcaSeguimiento.EN_ATENCION)))
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaSolicitar(jefe, incidencia(MarcaSeguimiento.EN_ATENCION, "demo-cmendoza")))
    }

    @Test
    fun `una incidencia cerrada no recibe solicitudes`() {
        assertEquals(ErrorDominio.IncidenciaCerrada, bloqueoParaSolicitar(luis, incidencia(MarcaSeguimiento.CERRADA)))
    }

    @Test
    fun `sin tecnico asignado no se solicita`() {
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaSolicitar(luis, incidencia(MarcaSeguimiento.SIN_ASIGNAR, null)))
    }

    @Test
    fun `pedir deja la incidencia esperando repuesto y si ya lo estaba no cambia`() {
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, marcaAlSolicitar(MarcaSeguimiento.EN_ATENCION))   // RF-1113
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, marcaAlSolicitar(MarcaSeguimiento.ASIGNADA))
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, marcaAlSolicitar(MarcaSeguimiento.ESPERANDO_REPUESTO))
    }

    @Test
    fun `con stock se ve disponible y sin stock se puede pedir igual`() {
        assertEquals(Disponibilidad.ConStock(3, 1), disponibilidadDe(stock = 3, diasEnvio = 1))   // RF-1105
        assertEquals(Disponibilidad.SinStock(5), disponibilidadDe(stock = 0, diasEnvio = 5))      // D-3
    }

    @Test
    fun `los dias de espera cuentan desde la solicitud hasta hoy`() {
        assertEquals(4, diasDeEspera(solicitud(), Instant.parse("2026-11-07T11:00:00Z")))   // RF-1122, D-9
    }

    @Test
    fun `entregada los dias de espera se detienen en la entrega`() {
        val entregada = solicitud(estado = EstadoEntrega.ENTREGADO, entregadaEn = Instant.parse("2026-11-05T10:00:00Z"))
        assertEquals(2, diasDeEspera(entregada, Instant.parse("2026-11-20T10:00:00Z")))
    }

    @Test
    fun `los dias de espera nunca son negativos`() {
        assertEquals(0, diasDeEspera(solicitud(), t0.minusSeconds(3600)))
    }

    @Test
    fun `con el ultimo repuesto entregado la incidencia vuelve a en atencion`() {
        assertEquals(MarcaSeguimiento.EN_ATENCION, marcaAlEntregar(MarcaSeguimiento.ESPERANDO_REPUESTO, quedanSinEntregar = 0))   // RF-1124
    }

    @Test
    fun `si queda otro repuesto por llegar la marca no cambia`() {
        assertNull(marcaAlEntregar(MarcaSeguimiento.ESPERANDO_REPUESTO, quedanSinEntregar = 1))
    }

    @Test
    fun `si la incidencia ya no espera repuesto la entrega no cambia la marca`() {
        assertNull(marcaAlEntregar(MarcaSeguimiento.CERRADA, quedanSinEntregar = 0))
        assertNull(marcaAlEntregar(MarcaSeguimiento.EN_ATENCION, quedanSinEntregar = 0))
    }

    @Test
    fun `el estado visible suma lo del celular a lo de Logistica`() {
        assertEquals(EstadoSolicitudVisible.POR_ENVIAR, estadoVisible(solicitud(numero = null)))   // RF-1120
        assertEquals(EstadoSolicitudVisible.POR_DESPACHAR, estadoVisible(solicitud()))
        assertEquals(EstadoSolicitudVisible.EN_CAMINO, estadoVisible(solicitud(estado = EstadoEntrega.EN_CAMINO)))
        assertEquals(EstadoSolicitudVisible.ENTREGADA, estadoVisible(solicitud(estado = EstadoEntrega.ENTREGADO)))
        assertEquals(EstadoSolicitudVisible.RECHAZADA, estadoVisible(solicitud(numero = null, rechazada = true)))   // RF-1119
    }
}
