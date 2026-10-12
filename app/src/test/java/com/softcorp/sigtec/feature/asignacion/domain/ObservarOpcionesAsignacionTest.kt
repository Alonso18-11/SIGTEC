package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.feature.asignacion.data.AsignacionRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-06 (RF-601 a RF-607): lo que recibe la pantalla de asignación. */
class ObservarOpcionesAsignacionTest {

    private val repo = AsignacionRepositoryFalso()
    private val observar = ObservarOpcionesAsignacion(repo)

    private val luis = Tecnico("u-luis", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
    private val ana = Tecnico("u-ana", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 4)

    private val incidencia = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `trae la incidencia con su oficina y los tecnicos por nombre`() = runTest {
        // RF-601
        repo.cargar(listOf(incidencia), listOf(luis, ana), oficinas = mapOf("IMP-RRHH-002" to "Recursos Humanos"))

        val opciones = observar("i127").first()!!

        assertEquals("Recursos Humanos", opciones.oficina)
        assertEquals(listOf("Ana Torres", "Luis Ramírez"), opciones.tecnicos.map { it.tecnico.nombre })
        assertNull(opciones.bloqueo)
    }

    @Test
    fun `la carga cambia al guardar una asignacion que aun no subio`() = runTest {
        // RF-602: Ana pasa de 4 a 5 de 5 y deja de estar disponible
        repo.cargar(listOf(incidencia), listOf(luis, ana))
        assertTrue(observar("i127").first()!!.tecnicos.first { it.tecnico.id == "u-ana" }.disponible)

        repo.fijarSinSubir(mapOf("u-ana" to 1))

        val opcionAna = observar("i127").first()!!.tecnicos.first { it.tecnico.id == "u-ana" }
        assertEquals(5, opcionAna.tecnico.incidenciasActivas)
        assertFalse(opcionAna.disponible)
    }

    @Test
    fun `una incidencia cerrada trae su bloqueo`() = runTest {
        // RF-607
        repo.cargar(listOf(incidencia.copy(marca = MarcaSeguimiento.CERRADA)), listOf(luis))
        assertEquals(ErrorDominio.IncidenciaCerrada, observar("i127").first()!!.bloqueo)
    }

    @Test
    fun `sin tecnicos descargados la lista viene vacia`() = runTest {
        // RF-606
        repo.cargar(listOf(incidencia))
        assertTrue(observar("i127").first()!!.tecnicos.isEmpty())
    }

    @Test
    fun `una incidencia que no esta en el celular no trae opciones`() = runTest {
        repo.cargar(listOf(incidencia), listOf(luis))
        assertNull(observar("no-existe").first())
    }
}
