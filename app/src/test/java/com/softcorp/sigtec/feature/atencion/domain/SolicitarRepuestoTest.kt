package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1102, RF-1103, RF-1110, RF-1113, RF-1115): el técnico asignado pide una pieza confirmada con huella. */
class SolicitarRepuestoTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val solicitar = SolicitarRepuesto(VerificarPermiso(sesion), repo)
    private val confirmadoEn = Instant.parse("2026-11-07T15:30:00Z")
    private val ahora = Instant.parse("2026-11-07T15:30:02Z")

    // INC-0124: asignada a Luis Ramírez, en atención
    private val enAtencion = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-03T09:15:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun nadaGuardado(incidencia: Incidencia = enAtencion) =
        repo.solicitudes.isEmpty() && repo.movimientos.isEmpty() && repo.incidencias == listOf(incidencia)

    private suspend fun solicitarValida(
        pieza: String = "Fuente de poder ATX 500 W",
        motivo: String = "No entrega energía tras el corte de luz",
        consultada: PiezaConsultada? = PiezaConsultada(stock = 3, diasEnvio = 1)
    ) = solicitar("i124", pieza, motivo, consultada, confirmadoEn, ahora)

    @Test
    fun `sin sesion no se guarda nada`() = runTest {
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), solicitarValida())
        assertTrue(nadaGuardado())
    }

    @Test
    fun `el jefe no solicita repuestos`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), solicitarValida())   // RF-1102
        assertTrue(nadaGuardado())
    }

    @Test
    fun `otro tecnico no solicita para una incidencia que no es suya`() = runTest {
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), solicitarValida())
        assertTrue(nadaGuardado())
    }

    @Test
    fun `una incidencia cerrada no recibe solicitudes`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val cerrada = enAtencion.copy(marca = MarcaSeguimiento.CERRADA)
        repo.cargar(listOf(cerrada))
        assertEquals(Resultado.Error(ErrorDominio.IncidenciaCerrada), solicitarValida())
        assertTrue(nadaGuardado(cerrada))
    }

    @Test
    fun `pieza o motivo vacios o con solo espacios no se guardan`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), solicitarValida(pieza = "   "))   // RF-1110
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), solicitarValida(motivo = ""))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `el camino feliz guarda la solicitud, el movimiento y la marca`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))

        val r = solicitarValida(pieza = "  Fuente de poder ATX 500 W ", motivo = " No entrega energía ")

        assertTrue(r is Resultado.Exito)
        val solicitud = repo.solicitudes.single()
        assertEquals("Fuente de poder ATX 500 W", solicitud.pieza)   // RF-1113: sin espacios en los extremos
        assertEquals("No entrega energía", solicitud.motivo)
        assertEquals(confirmadoEn, solicitud.confirmadoEn)
        assertEquals(ahora, solicitud.fechaSolicitud)
        assertEquals(EstadoEntrega.POR_DESPACHAR, solicitud.estadoEntrega)
        assertEquals("demo-lramirez", solicitud.solicitadoPorId)
        assertEquals("Luis Ramírez", solicitud.solicitadoPorNombre)
        assertEquals(3, solicitud.stockConsultado)
        assertEquals(1, solicitud.diasEnvio)
        assertNull(solicitud.numeroLogistica)   // RF-1114: «Por enviar a Logística»

        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.REPUESTO_SOLICITADO, movimiento.tipo)
        assertEquals("Fuente de poder ATX 500 W", movimiento.detalle)
        assertEquals("Luis Ramírez", movimiento.autorNombre)

        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, repo.incidencias.single().marca)
        assertEquals(ahora, repo.ultimoAhora)
    }

    @Test
    fun `sin consulta a Logistica se guarda igual sin disponibilidad`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))

        solicitarValida(consultada = null)   // D-2

        assertNull(repo.solicitudes.single().stockConsultado)
    }

    @Test
    fun `con otra pieza ya esperando se agrega una solicitud mas sin cambiar la marca`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion.copy(marca = MarcaSeguimiento.ESPERANDO_REPUESTO)))

        val r = solicitarValida(pieza = "Cable HDMI 1,8 m")

        assertTrue(r is Resultado.Exito)   // RF-1103
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, repo.incidencias.single().marca)
        assertEquals(1, repo.solicitudes.size)
    }

    @Test
    fun `si Room falla no queda nada a medias y se propaga el error`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        repo.fallar = true

        val r = solicitarValida()

        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)   // RF-1115
        assertTrue(nadaGuardado())
    }
}
