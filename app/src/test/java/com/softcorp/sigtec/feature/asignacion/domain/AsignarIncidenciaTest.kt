package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.asignacion.data.AsignacionRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-06 (RF-608 a RF-616, RF-625): asignar y reasignar desde el dominio, solo en Room. */
class AsignarIncidenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AsignacionRepositoryFalso()
    private val asignar = AsignarIncidencia(VerificarPermiso(sesion), repo)
    private val ahora = Instant.parse("2026-10-11T15:32:00Z")

    private val luis = Tecnico("demo-lramirez", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
    private val ana = Tecnico("demo-atorres", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 4)

    // INC-0127 del prototipo: pendiente, con número y sin técnico
    private val sinAsignar = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )
    private val asignadaAAna = sinAsignar.copy(
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = ana.id, tecnicoAsignadoNombre = ana.nombre
    )

    private suspend fun comoJefe(incidencia: Incidencia = sinAsignar) {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(listOf(incidencia), listOf(luis, ana))
    }

    private fun nadaGuardado() = repo.movimientos.isEmpty() && repo.ultimoAhora == null

    @Test
    fun `sin sesion no asigna`() = runTest {
        repo.cargar(listOf(sinAsignar), listOf(luis))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), asignar("i127", luis.id, ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `un tecnico no tiene permiso para asignar`() = runTest {
        // RF-608
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(sinAsignar), listOf(luis))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), asignar("i127", luis.id, ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `una incidencia cerrada no se asigna`() = runTest {
        // RF-609
        comoJefe(sinAsignar.copy(marca = MarcaSeguimiento.CERRADA))
        assertEquals(Resultado.Error(ErrorDominio.IncidenciaCerrada), asignar("i127", luis.id, ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `una incidencia por sincronizar no se asigna`() = runTest {
        // RF-610
        comoJefe(sinAsignar.copy(numero = null))
        assertEquals(Resultado.Error(ErrorDominio.IncidenciaPorSincronizar), asignar("i127", luis.id, ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `no se reasigna al mismo tecnico`() = runTest {
        // RF-611
        comoJefe(asignadaAAna)
        assertEquals(Resultado.Error(ErrorDominio.YaAsignada("Ana Torres")), asignar("i127", ana.id, ahora))
        assertTrue(nadaGuardado())
    }

    @Test
    fun `el limite cuenta las asignaciones que aun no subieron`() = runTest {
        // RF-612, RF-602: Ana tiene 4 de 5 en el servidor y 1 más asignada en este celular sin subir
        comoJefe()
        repo.fijarSinSubir(mapOf(ana.id to 1))
        assertEquals(
            Resultado.Error(ErrorDominio.LimiteAtencionAlcanzado("Ana Torres")),
            asignar("i127", ana.id, ahora)
        )
        assertTrue(nadaGuardado())
    }

    @Test
    fun `asigna una incidencia sin tecnico con su movimiento ASIGNADA`() = runTest {
        // RF-613, RF-614, RF-615
        comoJefe()

        val r = asignar("i127", luis.id, ahora)

        val asignada = (r as Resultado.Exito).valor
        assertEquals(MarcaSeguimiento.ASIGNADA, asignada.marca)
        assertEquals(luis.id, asignada.tecnicoAsignadoId)
        assertEquals("Luis Ramírez", asignada.tecnicoAsignadoNombre)
        assertEquals(listOf(asignada), repo.incidencias)
        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.ASIGNADA, movimiento.tipo)
        assertEquals("i127", movimiento.incidenciaId)
        assertEquals("Carlos Mendoza", movimiento.autorNombre)
        assertEquals(ahora, movimiento.fecha)
        assertEquals("A Luis Ramírez, por Carlos Mendoza", movimiento.detalle)
        assertEquals(ahora, repo.ultimoAhora)
    }

    @Test
    fun `reasignar desde en atencion vuelve a asignada con movimiento REASIGNADA`() = runTest {
        // RF-614, RF-615
        comoJefe(asignadaAAna)

        val asignada = (asignar("i127", luis.id, ahora) as Resultado.Exito).valor

        assertEquals(MarcaSeguimiento.ASIGNADA, asignada.marca)
        assertEquals(luis.id, asignada.tecnicoAsignadoId)
        val movimiento = repo.movimientos.single()
        assertEquals(TipoMovimiento.REASIGNADA, movimiento.tipo)
        assertEquals("De Ana Torres a Luis Ramírez, por Carlos Mendoza", movimiento.detalle)
    }

    @Test
    fun `reasignar desde esperando repuesto conserva la marca`() = runTest {
        // RF-625: la pieza sigue en camino
        comoJefe(asignadaAAna.copy(marca = MarcaSeguimiento.ESPERANDO_REPUESTO))

        val asignada = (asignar("i127", luis.id, ahora) as Resultado.Exito).valor

        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, asignada.marca)
        assertEquals(luis.id, asignada.tecnicoAsignadoId)
        assertEquals(TipoMovimiento.REASIGNADA, repo.movimientos.single().tipo)
    }

    @Test
    fun `si Room falla devuelve el error y no guarda nada`() = runTest {
        // RF-616
        comoJefe()
        repo.fallar = true

        val r = asignar("i127", luis.id, ahora)

        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)
        assertEquals(listOf(sinAsignar), repo.incidencias)
        assertTrue(nadaGuardado())
    }

    @Test
    fun `una incidencia que no esta en el celular devuelve un error desconocido`() = runTest {
        // D-5 del plan
        comoJefe()
        val r = asignar("no-existe", luis.id, ahora)
        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)
        assertTrue(nadaGuardado())
    }
}
