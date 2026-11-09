package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.diccionario.data.DiccionarioRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-13 (RF-1308, RF-1309, RF-1313, RF-1314, RF-1318): registrar y consultar según el perfil. */
class RegistrarFallaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = DiccionarioRepositoryFalso()
    private val registrar = RegistrarFalla(VerificarPermiso(sesion), repo)
    private val observar = ObservarDiccionario(VerificarPermiso(sesion), repo)
    private val ahora = Instant.parse("2026-11-09T15:00:00Z")

    private val datos = DatosFalla(
        titulo = "  La impresora no aparece en la red ", tipo = TipoFalla.RED, componente = " Impresora ",
        sintomas = " No imprime desde las PC ", solucion = " Asignar una IP fija "
    )

    @Test
    fun `sin sesion no guarda`() = runTest {
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), registrar(datos, ahora))
        assertTrue(repo.fallas.isEmpty())
    }

    @Test
    fun `el personal de sistemas y el jefe no registran`() = runTest {
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), registrar(datos, ahora))
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), registrar(datos, ahora))
        assertTrue(repo.fallas.isEmpty())
    }

    @Test
    fun `un campo vacio o sin tipo no se guarda`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), registrar(datos.copy(componente = "   "), ahora))
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), registrar(datos.copy(tipo = null), ahora))
        assertTrue(repo.fallas.isEmpty())
    }

    @Test
    fun `el tecnico guarda la falla recortada a su nombre y con el origen`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")

        val r = registrar(datos, ahora, incidenciaOrigenId = "inc-1")

        val falla = (r as Resultado.Exito).valor
        assertEquals("La impresora no aparece en la red", falla.titulo)
        assertEquals("Impresora", falla.componente)
        assertEquals("No imprime desde las PC", falla.sintomas)
        assertEquals("Asignar una IP fija", falla.solucion)
        assertEquals("demo-lramirez", falla.autorId)
        assertEquals("Luis Ramírez", falla.autorNombre)
        assertEquals(ahora, falla.registradaEn)
        assertEquals("inc-1", falla.incidenciaOrigenId)
        assertEquals(listOf(falla), repo.fallas)
        assertTrue(repo.existeConOrigen("inc-1").first())
    }

    @Test
    fun `si el repositorio falla el error se propaga`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.fallarAlGuardar = true
        assertTrue((registrar(datos, ahora) as Resultado.Error).causa is ErrorDominio.Desconocido)
    }

    @Test
    fun `el jefe no consulta el diccionario`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), observar())
    }

    @Test
    fun `el tecnico consulta y puede registrar, el personal de sistemas solo consulta`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertTrue((observar() as Resultado.Exito).valor.puedeRegistrar)
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        assertFalse((observar() as Resultado.Exito).valor.puedeRegistrar)
    }
}
