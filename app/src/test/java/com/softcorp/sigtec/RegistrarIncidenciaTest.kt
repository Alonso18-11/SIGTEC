package com.softcorp.sigtec

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.RegistrarIncidencia
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarIncidenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = IncidenciaRepositoryFalso()
    private val registrar = RegistrarIncidencia(VerificarPermiso(sesion), repo)

    private suspend fun registrarComoLuis(): Incidencia {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val r = registrar("  pc-cont-014 ", "  No enciende tras el corte de luz  ", " María Quispe ")
        return (r as Resultado.Exito).valor
    }

    @Test
    fun `sin sesion no registra`() = runTest {
        val r = registrar("PC-CONT-014", "No enciende", "María Quispe")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), r)
        assertTrue(repo.incidencias.isEmpty() && repo.movimientos.isEmpty())
    }

    @Test
    fun `recorta espacios y pasa el codigo a mayusculas`() = runTest {
        val inc = registrarComoLuis()
        assertEquals("PC-CONT-014", inc.codigoEquipo)
        assertEquals("No enciende tras el corte de luz", inc.descripcion)
        assertEquals("María Quispe", inc.usuarioResponsable)
    }

    @Test
    fun `nace sin asignar, sin numero y a nombre de quien registra`() = runTest {
        val inc = registrarComoLuis()
        assertEquals(MarcaSeguimiento.SIN_ASIGNAR, inc.marca)
        assertEquals(EstadoIncidencia.PENDIENTE, inc.estado)
        assertNull(inc.numero)
        assertNull(inc.codigoVisible)
        assertEquals("demo-lramirez", inc.registradoPorId)
        assertEquals("Luis Ramírez", inc.registradoPorNombre)
        assertEquals(listOf(inc), repo.incidencias)
    }

    @Test
    fun `registra el movimiento REGISTRADA de la misma incidencia`() = runTest {
        val inc = registrarComoLuis()
        val mov = repo.movimientos.single()
        assertEquals(TipoMovimiento.REGISTRADA, mov.tipo)
        assertEquals(inc.id, mov.incidenciaId)
        assertEquals("Luis Ramírez", mov.autorNombre)
        assertEquals(inc.fechaRegistro, mov.fecha)
    }

    @Test
    fun `si Room falla devuelve el error`() = runTest {
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        repo.fallar = true
        val r = registrar("PC-CONT-014", "No enciende", "María Quispe")
        assertTrue(r is Resultado.Error)
    }
}
