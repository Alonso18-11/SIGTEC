package com.softcorp.sigtec

import com.softcorp.sigtec.core.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ModelosDominioTest {

    @Test
    fun `tecnico en su limite no esta disponible`() {
        val ana = Tecnico("t2", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 5)
        assertFalse(ana.disponible)
    }

    @Test
    fun `tecnico bajo su limite esta disponible`() {
        val luis = Tecnico("t1", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
        assertTrue(luis.disponible)
    }

    @Test
    fun `solo la marca CERRADA significa Solucionado`() {
        val base = incidencia(MarcaSeguimiento.ESPERANDO_REPUESTO)
        assertEquals(EstadoIncidencia.PENDIENTE, base.estado)
        assertEquals(EstadoIncidencia.SOLUCIONADO, base.copy(marca = MarcaSeguimiento.CERRADA).estado)
    }

    @Test
    fun `sin numero no hay codigo visible`() {
        assertNull(incidencia(MarcaSeguimiento.SIN_ASIGNAR).codigoVisible)
        assertEquals("INC-0124", incidencia(MarcaSeguimiento.SIN_ASIGNAR).copy(numero = 124).codigoVisible)
    }

    @Test
    fun `el tecnico no puede asignar incidencias`() {
        assertFalse(Perfil.TECNICO.puede(Permiso.ASIGNAR_INCIDENCIA))
        assertTrue(Perfil.JEFE.puede(Permiso.ASIGNAR_INCIDENCIA))
    }

    private fun incidencia(marca: MarcaSeguimiento) = Incidencia(
        id = "uuid-1", numero = null, codigoEquipo = "PC-CONT-014",
        descripcion = "No enciende", usuarioResponsable = "María Quispe",
        fechaRegistro = Instant.now(), registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = marca
    )
}