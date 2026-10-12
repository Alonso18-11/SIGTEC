package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
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

/** HU-08 (RF-812 a RF-820): el técnico asignado cierra su incidencia con el informe confirmado por huella. */
class CerrarIncidenciaConInformeTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val cerrar = CerrarIncidenciaConInforme(VerificarPermiso(sesion), repo)
    private val confirmadoEn = Instant.parse("2026-10-12T15:30:00Z")

    // INC-0127: asignada a Luis Ramírez, en atención
    private val enAtencion = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun nadaGuardado() =
        repo.movimientos.isEmpty() && repo.informes.isEmpty() && repo.ultimoAhora == null &&
            repo.incidencias == listOf(enAtencion)

    private suspend fun cerrarValido(incidenciaId: String = "i127") = cerrar(
        incidenciaId = incidenciaId,
        diagnostico = "Fusor dañado",
        trabajoRealizado = "Se cambió el fusor",
        piezasCambiadas = "Fusor",
        observaciones = null,
        confirmadoEn = confirmadoEn
    )

    @Test
    fun `sin sesion no cierra la incidencia`() = runTest {
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), cerrarValido())
        assertTrue(nadaGuardado())
    }

    @Test
    fun `un tecnico distinto al asignado no puede cerrar`() = runTest {
        // RF-812
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), cerrarValido())
        assertTrue(nadaGuardado())
    }

    @Test
    fun `no se puede cerrar una incidencia sin asignar`() = runTest {
        // RF-812: cambió por otra vía justo antes de guardarse (el técnico sigue siendo el mismo)
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val sinAsignar = enAtencion.copy(marca = MarcaSeguimiento.SIN_ASIGNAR)
        repo.cargar(listOf(sinAsignar))
        val r = cerrarValido()
        assertEquals(Resultado.Error(ErrorDominio.MarcaCambiada), r)
        assertTrue(repo.movimientos.isEmpty() && repo.informes.isEmpty() && repo.ultimoAhora == null)
        assertEquals(listOf(sinAsignar), repo.incidencias)
    }

    @Test
    fun `no se puede cerrar una incidencia ya cerrada`() = runTest {
        // RF-812
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val cerrada = enAtencion.copy(marca = MarcaSeguimiento.CERRADA)
        repo.cargar(listOf(cerrada))
        val r = cerrarValido()
        assertEquals(Resultado.Error(ErrorDominio.MarcaCambiada), r)
        assertTrue(repo.movimientos.isEmpty() && repo.informes.isEmpty() && repo.ultimoAhora == null)
        assertEquals(listOf(cerrada), repo.incidencias)
    }

    @Test
    fun `se puede cerrar desde Asignada`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion.copy(marca = MarcaSeguimiento.ASIGNADA)))
        val r = cerrarValido()
        assertTrue(r is Resultado.Exito)
        assertEquals(MarcaSeguimiento.CERRADA, (r as Resultado.Exito).valor.marca)
    }

    @Test
    fun `se puede cerrar desde En atencion`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        val r = cerrarValido()
        assertTrue(r is Resultado.Exito)
        assertEquals(MarcaSeguimiento.CERRADA, (r as Resultado.Exito).valor.marca)
    }

    @Test
    fun `se puede cerrar desde Esperando repuesto`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion.copy(marca = MarcaSeguimiento.ESPERANDO_REPUESTO)))
        val r = cerrarValido()
        assertTrue(r is Resultado.Exito)
        assertEquals(MarcaSeguimiento.CERRADA, (r as Resultado.Exito).valor.marca)
    }

    @Test
    fun `diagnostico vacio tras recortar no guarda nada`() = runTest {
        // RF-818
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        val r = cerrar("i127", "   ", "Se cambió el fusor", null, null, confirmadoEn)
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), r)
        assertTrue(nadaGuardado())
    }

    @Test
    fun `solucion aplicada vacia tras recortar no guarda nada`() = runTest {
        // RF-818
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        val r = cerrar("i127", "Fusor dañado", "   ", null, null, confirmadoEn)
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), r)
        assertTrue(nadaGuardado())
    }

    @Test
    fun `piezas y observaciones en blanco quedan null`() = runTest {
        // RF-818
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        val r = cerrar("i127", "Fusor dañado", "Se cambió el fusor", "   ", "   ", confirmadoEn)
        assertTrue(r is Resultado.Exito)
        val informe = repo.informes.single()
        assertNull(informe.piezasCambiadas)
        assertNull(informe.observaciones)
    }

    @Test
    fun `camino feliz guarda la incidencia cerrada el movimiento y el informe`() = runTest {
        // RF-819
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))

        val r = cerrarValido()

        val cerrada = (r as Resultado.Exito).valor
        assertEquals(MarcaSeguimiento.CERRADA, cerrada.marca)
        assertEquals(listOf(cerrada), repo.incidencias)

        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.CERRADA, movimiento.tipo)
        assertEquals("i127", movimiento.incidenciaId)
        assertEquals("Luis Ramírez", movimiento.autorNombre)
        assertEquals(confirmadoEn, movimiento.fecha)

        val informe = repo.informes.single()
        assertEquals("i127", informe.incidenciaId)
        assertEquals("Fusor dañado", informe.diagnostico)
        assertEquals("Se cambió el fusor", informe.trabajoRealizado)
        assertEquals("Fusor", informe.piezasCambiadas)
        assertEquals(confirmadoEn, informe.fechaCierre)
        assertEquals(confirmadoEn, informe.confirmadoEn)

        assertEquals(confirmadoEn, repo.ultimoAhora)
    }

    @Test
    fun `si Room falla devuelve el error y no guarda nada`() = runTest {
        // RF-820
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(enAtencion))
        repo.fallar = true

        val r = cerrarValido()

        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)
        assertTrue(nadaGuardado())
    }
}
