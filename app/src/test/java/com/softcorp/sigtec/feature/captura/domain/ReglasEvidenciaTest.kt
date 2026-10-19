package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-09 (RF-923, RF-926, RF-929, RF-931, RF-937): quién agrega o elimina fotos y hasta cuántas. */
class ReglasEvidenciaTest {

    private val luis = Usuario("demo-lramirez", "Luis Ramírez", "lramirez@softcorp.pe", "Sistemas", "Técnico", Perfil.TECNICO)
    private val ana = Usuario("demo-atorres", "Ana Torres", "atorres@softcorp.pe", "Sistemas", "Técnica", Perfil.TECNICO)
    private val jefe = Usuario("demo-cmendoza", "Carlos Mendoza", "cmendoza@softcorp.pe", "Sistemas", "Jefe", Perfil.JEFE)

    // INC-0124: asignada a Luis, en atención
    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = luis.id, tecnicoAsignadoNombre = luis.nombre
    )

    @Test
    fun `sin sesion no puede`() {
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaFotos(null, deLuis, cantidad = 0))
    }

    @Test
    fun `sin el permiso de atender no puede aunque figure como asignado`() {
        val asignadaAlJefe = deLuis.copy(tecnicoAsignadoId = jefe.id)
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaFotos(jefe, asignadaAlJefe, cantidad = 0))
    }

    @Test
    fun `otro tecnico no puede`() {
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaFotos(ana, deLuis, cantidad = 0))
    }

    @Test
    fun `sin la incidencia no puede`() {
        assertEquals(ErrorDominio.SinPermiso, bloqueoParaFotos(luis, null, cantidad = 0))
    }

    @Test
    fun `con la incidencia cerrada no agrega ni elimina`() {
        val cerrada = deLuis.copy(marca = MarcaSeguimiento.CERRADA)
        assertEquals(ErrorDominio.IncidenciaCerrada, bloqueoParaFotos(luis, cerrada, cantidad = 3))
        assertEquals(ErrorDominio.IncidenciaCerrada, bloqueoParaFotos(luis, cerrada, cantidad = null))   // RF-937
    }

    @Test
    fun `con 10 fotos ya no agrega`() {
        assertEquals(ErrorDominio.LimiteFotosAlcanzado, bloqueoParaFotos(luis, deLuis, cantidad = MAX_FOTOS))
    }

    @Test
    fun `el tecnico asignado agrega hasta la novena foto`() {
        assertNull(bloqueoParaFotos(luis, deLuis, cantidad = 0))
        assertNull(bloqueoParaFotos(luis, deLuis, cantidad = MAX_FOTOS - 1))
        // Esperando un repuesto la incidencia sigue pendiente
        assertNull(bloqueoParaFotos(luis, deLuis.copy(marca = MarcaSeguimiento.ESPERANDO_REPUESTO), cantidad = 5))
    }

    @Test
    fun `al eliminar el maximo no aplica`() {
        assertNull(bloqueoParaFotos(luis, deLuis, cantidad = null))
    }
}
