package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.data.LogisticaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1123, RF-1124): el estado de Logística llega a las solicitudes y el último repuesto retoma la atención. */
class ActualizarEstadoRepuestosTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val logistica = LogisticaRepositoryFalso()
    private val actualizar = ActualizarEstadoRepuestos(VerificarPermiso(sesion), repo, logistica)
    private val ahora = Instant.parse("2026-11-09T10:00:00Z")
    private val entregadaEn = Instant.parse("2026-11-09T09:00:00Z")

    private val esperando = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-03T09:15:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.ESPERANDO_REPUESTO, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun solicitud(id: String, numero: String?, estado: EstadoEntrega = EstadoEntrega.POR_DESPACHAR) = SolicitudRepuesto(
        id = id, incidenciaId = "i124", pieza = "Pieza $id", motivo = "Motivo",
        fechaSolicitud = Instant.parse("2026-11-07T10:00:00Z"), confirmadoEn = Instant.parse("2026-11-07T10:00:00Z"),
        estadoEntrega = estado, solicitadoPorId = "demo-lramirez", solicitadoPorNombre = "Luis Ramírez",
        numeroLogistica = numero
    )

    private fun estado(e: EstadoEntrega, entregada: Instant? = null) =
        EstadoEnLogistica(e, despachadaEn = Instant.parse("2026-11-08T10:00:00Z"), entregadaEn = entregada, llegadaEstimada = null)

    @Test
    fun `sin sesion no consulta nada`() = runTest {
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), actualizar(ahora))
    }

    @Test
    fun `un cambio a en camino se guarda sin tocar la incidencia`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando))
        repo.cargarSolicitudes(listOf(solicitud("a", "17")))
        logistica.estados["17"] = estado(EstadoEntrega.EN_CAMINO)

        val r = actualizar(ahora)

        assertEquals(Resultado.Exito(ResultadoActualizacion(actualizadas = 1, sinRespuesta = 0)), r)   // RF-1123
        assertEquals(EstadoEntrega.EN_CAMINO, repo.solicitudes.single().estadoEntrega)
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, repo.incidencias.single().marca)
        assertTrue(repo.movimientos.isEmpty())
    }

    @Test
    fun `entregado el ultimo repuesto la incidencia vuelve a en atencion con su movimiento`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando))
        repo.cargarSolicitudes(listOf(solicitud("a", "17"), solicitud("b", "18", EstadoEntrega.ENTREGADO)))
        logistica.estados["17"] = estado(EstadoEntrega.ENTREGADO, entregadaEn)

        actualizar(ahora)

        assertEquals(MarcaSeguimiento.EN_ATENCION, repo.incidencias.single().marca)   // RF-1124, D-7
        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.REPUESTO_RECIBIDO, movimiento.tipo)
        assertEquals("Pieza a", movimiento.detalle)
        assertEquals(entregadaEn, movimiento.fecha)
        assertEquals(entregadaEn, repo.solicitudes.first { it.id == "a" }.entregadaEn)
    }

    @Test
    fun `si queda otro repuesto por llegar la marca no cambia`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando))
        repo.cargarSolicitudes(listOf(solicitud("a", "17"), solicitud("b", null)))   // la b ni siquiera salió del celular
        logistica.estados["17"] = estado(EstadoEntrega.ENTREGADO, entregadaEn)

        actualizar(ahora)

        assertEquals(EstadoEntrega.ENTREGADO, repo.solicitudes.first { it.id == "a" }.estadoEntrega)
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, repo.incidencias.single().marca)
        assertTrue(repo.movimientos.isEmpty())
    }

    @Test
    fun `con la incidencia ya cerrada solo se actualiza la solicitud`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando.copy(marca = MarcaSeguimiento.CERRADA)))
        repo.cargarSolicitudes(listOf(solicitud("a", "17")))
        logistica.estados["17"] = estado(EstadoEntrega.ENTREGADO, entregadaEn)

        actualizar(ahora)

        assertEquals(EstadoEntrega.ENTREGADO, repo.solicitudes.single().estadoEntrega)
        assertEquals(MarcaSeguimiento.CERRADA, repo.incidencias.single().marca)
        assertTrue(repo.movimientos.isEmpty())
    }

    @Test
    fun `sin cambios no guarda nada`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando))
        repo.cargarSolicitudes(listOf(solicitud("a", "17")))   // Logística responde «por despachar», sin fechas

        val r = actualizar(ahora)

        assertEquals(Resultado.Exito(ResultadoActualizacion(actualizadas = 0, sinRespuesta = 0)), r)
    }

    @Test
    fun `una consulta que falla se cuenta y la ultima consulta correcta no se registra`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(esperando))
        repo.cargarSolicitudes(listOf(solicitud("a", "17")))
        logistica.fallaConsulta = ErrorLogistica.SIN_RESPUESTA

        val r = actualizar(ahora)

        assertEquals(Resultado.Exito(ResultadoActualizacion(actualizadas = 0, sinRespuesta = 1)), r)
        assertEquals(listOf(ahora to false), repo.consultas)   // RF-1123: la hora mostrada sigue siendo la anterior
    }

    @Test
    fun `una consulta correcta queda registrada aunque no haya cambios`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")

        actualizar(ahora)

        assertEquals(listOf(ahora to true), repo.consultas)
    }
}
