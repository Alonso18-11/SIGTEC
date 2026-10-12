package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-08 (RF-806 a RF-811): el técnico asignado marca que empezó a atender su incidencia. */
class IniciarAtencionTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = IncidenciaRepositoryFalso()
    private val iniciarAtencion = IniciarAtencion(VerificarPermiso(sesion), repo)
    private val ahora = Instant.parse("2026-10-12T09:15:00Z")

    // INC-0127: asignada a Luis Ramírez, lista para que empiece a atenderla
    private val asignada = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.ASIGNADA, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun nadaGuardado() =
        repo.movimientos.isEmpty() && repo.ultimoAhora == null && repo.incidencias == listOf(asignada)

    @Test
    fun `sin sesion no inicia la atencion`() = runTest {
        repo.cargar(listOf(asignada))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), iniciarAtencion("i127", ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `un tecnico distinto al asignado no puede iniciar la atencion`() = runTest {
        // RF-807
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        repo.cargar(listOf(asignada))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), iniciarAtencion("i127", ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `si la marca ya no es asignada no guarda nada`() = runTest {
        // RF-808: cambió por otra vía justo antes de guardarse
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val enAtencion = asignada.copy(marca = MarcaSeguimiento.EN_ATENCION)
        repo.cargar(listOf(enAtencion))
        val r = iniciarAtencion("i127", ahora)
        assertEquals(Resultado.Error(ErrorDominio.MarcaCambiada), r)
        assertTrue(repo.movimientos.isEmpty() && repo.ultimoAhora == null)
        assertEquals(listOf(enAtencion), repo.incidencias)
    }

    @Test
    fun `el tecnico asignado inicia la atencion y queda En atencion`() = runTest {
        // RF-809
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(asignada))

        val r = iniciarAtencion("i127", ahora)

        val enAtencion = (r as Resultado.Exito).valor
        assertEquals(MarcaSeguimiento.EN_ATENCION, enAtencion.marca)
        assertEquals(listOf(enAtencion), repo.incidencias)
        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.ATENCION_INICIADA, movimiento.tipo)
        assertEquals("i127", movimiento.incidenciaId)
        assertEquals("Luis Ramírez", movimiento.autorNombre)
        assertEquals(ahora, movimiento.fecha)
        assertEquals(ahora, repo.ultimoAhora)
    }

    @Test
    fun `si Room falla devuelve el error y no guarda nada`() = runTest {
        // RF-810
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(asignada))
        repo.fallar = true

        val r = iniciarAtencion("i127", ahora)

        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)
        assertTrue(nadaGuardado())
    }
}
