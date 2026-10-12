package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-06: reglas puras de la asignación, sin Room ni sesión. */
class ReglasAsignacionTest {

    private val luis = Tecnico("u-luis", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
    private val ana = Tecnico("u-ana", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 4)

    private val incidencia = Incidencia(
        id = "i1", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:00:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `la carga efectiva suma las asignaciones que aun no subieron`() {
        // RF-602: 3 en el servidor + 1 sin subir = 4 de 5, todavía disponible
        val efectivo = cargaEfectiva(luis, sinSubir = 1)
        assertEquals(4, efectivo.incidenciasActivas)
        assertTrue(efectivo.disponible)
    }

    @Test
    fun `con la asignacion sin subir llega a su limite y deja de estar disponible`() {
        // RF-602, RF-612: 4 en el servidor + 1 sin subir = 5 de 5
        assertFalse(cargaEfectiva(ana, sinSubir = 1).disponible)
    }

    @Test
    fun `una incidencia pendiente y con numero se puede asignar`() {
        assertNull(bloqueoParaAsignar(incidencia))
    }

    @Test
    fun `una incidencia cerrada no se puede asignar`() {
        // RF-609
        val cerrada = incidencia.copy(marca = MarcaSeguimiento.CERRADA)
        assertEquals(ErrorDominio.IncidenciaCerrada, bloqueoParaAsignar(cerrada))
    }

    @Test
    fun `una incidencia por sincronizar no se puede asignar`() {
        // RF-610
        assertEquals(ErrorDominio.IncidenciaPorSincronizar, bloqueoParaAsignar(incidencia.copy(numero = null)))
    }

    @Test
    fun `cerrada pesa mas que por sincronizar`() {
        val ambas = incidencia.copy(marca = MarcaSeguimiento.CERRADA, numero = null)
        assertEquals(ErrorDominio.IncidenciaCerrada, bloqueoParaAsignar(ambas))
    }

    @Test
    fun `al asignar o reasignar queda asignada salvo que espere un repuesto`() {
        // RF-614
        assertEquals(MarcaSeguimiento.ASIGNADA, marcaAlAsignar(MarcaSeguimiento.SIN_ASIGNAR))
        assertEquals(MarcaSeguimiento.ASIGNADA, marcaAlAsignar(MarcaSeguimiento.ASIGNADA))
        assertEquals(MarcaSeguimiento.ASIGNADA, marcaAlAsignar(MarcaSeguimiento.EN_ATENCION))
        // RF-625: la pieza sigue en camino
        assertEquals(MarcaSeguimiento.ESPERANDO_REPUESTO, marcaAlAsignar(MarcaSeguimiento.ESPERANDO_REPUESTO))
    }

    @Test
    fun `sin tecnico es asignacion y con otro es reasignacion`() {
        // RF-613
        assertEquals(TipoMovimiento.ASIGNADA, tipoDeAsignacion(null))
        assertEquals(TipoMovimiento.REASIGNADA, tipoDeAsignacion("u-ana"))
    }

    @Test
    fun `el detalle dice a quien y quien asigno`() {
        // RF-615
        assertEquals("A Luis Ramírez, por Carlos Mendoza", detalleDeAsignacion(null, "Luis Ramírez", "Carlos Mendoza"))
        assertEquals(
            "De Ana Torres a Luis Ramírez, por Carlos Mendoza",
            detalleDeAsignacion("Ana Torres", "Luis Ramírez", "Carlos Mendoza")
        )
    }

    @Test
    fun `las opciones van por nombre con su carga efectiva y marcan al actual`() {
        // RF-601 a RF-604
        val asignadaAAna = incidencia.copy(tecnicoAsignadoId = "u-ana", tecnicoAsignadoNombre = "Ana Torres")

        val opciones = armarOpciones(asignadaAAna, listOf(luis, ana), sinSubir = mapOf("u-ana" to 1))

        assertEquals(listOf("Ana Torres", "Luis Ramírez"), opciones.map { it.tecnico.nombre })
        val opcionAna = opciones.first()
        assertEquals(5, opcionAna.tecnico.incidenciasActivas)
        assertFalse(opcionAna.disponible)
        assertTrue(opcionAna.esActual)
        val opcionLuis = opciones.last()
        assertEquals(3, opcionLuis.tecnico.incidenciasActivas)
        assertTrue(opcionLuis.disponible)
        assertFalse(opcionLuis.esActual)
    }
}
