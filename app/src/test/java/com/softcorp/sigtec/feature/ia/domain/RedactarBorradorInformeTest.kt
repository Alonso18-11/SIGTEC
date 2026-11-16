package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.ia.data.AsistenteIAFalso
import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-14 (RF-1416, RF-1417, RF-1419, RF-1422, RF-1423): el borrador lo pide el técnico asignado, desde sus notas. */
class RedactarBorradorInformeTest {

    private val sesion = SesionRepositoryDemo()
    private val asistente = AsistenteIAFalso()
    private val repo = SugerenciaIARepositoryFalso()
    private val redactar = RedactarBorradorInforme(VerificarPermiso(sesion), asistente, repo)
    private val ahora = Instant.parse("2026-11-16T16:00:00Z")

    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende después del corte de luz",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-16T09:15:00Z"),
        registradoPorId = "demo-lramirez", registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )
    private val entrada = EntradaBorrador(descripcion = deLuis.descripcion, notas = "cambie la fuente, ahora prende")

    private suspend fun comoLuis() = sesion.iniciarSesion("lramirez@softcorp.pe", "")

    private fun borradorAnterior() = SugerenciaIA(
        "b-viejo", "i124", TipoSugerencia.BORRADOR_INFORME, "{}", "m", ahora.minusSeconds(600)
    )

    @Test
    fun `otro tecnico o el jefe no pueden pedirlo`() = runTest {
        // RF-1416
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), redactar(deLuis, entrada, ahora))
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), redactar(deLuis, entrada, ahora))
        assertTrue(asistente.entradasDeBorrador.isEmpty())
    }

    @Test
    fun `una incidencia cerrada no se redacta`() = runTest {
        comoLuis()
        assertEquals(
            Resultado.Error(ErrorDominio.SinPermiso),
            redactar(deLuis.copy(marca = MarcaSeguimiento.CERRADA), entrada, ahora)
        )
    }

    @Test
    fun `sin notas no llama a Gemini`() = runTest {
        // RF-1415
        comoLuis()
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), redactar(deLuis, entrada.copy(notas = "  "), ahora))
        assertTrue(asistente.entradasDeBorrador.isEmpty())
    }

    @Test
    fun `si Gemini falla, el borrador anterior sigue sin decidir`() = runTest {
        // RF-1423
        comoLuis()
        repo.cargar(borradorAnterior())
        asistente.respuestaBorrador = Resultado.Error(ErrorDominio.IANoDisponible)

        assertEquals(Resultado.Error(ErrorDominio.IANoDisponible), redactar(deLuis, entrada, ahora))
        assertNull(repo.sugerencias.single().aceptada)
    }

    @Test
    fun `una respuesta sin diagnostico no sirve`() = runTest {
        comoLuis()
        asistente.respuestaBorrador = Resultado.Exito("""{"diagnostico": "", "trabajoRealizado": "Se cambió"}""")
        assertEquals(Resultado.Error(ErrorDominio.IANoDisponible), redactar(deLuis, entrada, ahora))
        assertTrue(repo.sugerencias.isEmpty())
    }

    @Test
    fun `un borrador nuevo deja el anterior como no aceptado`() = runTest {
        // RF-1422
        comoLuis()
        repo.cargar(borradorAnterior())

        val nuevo = (redactar(deLuis, entrada, ahora) as Resultado.Exito).valor

        val anterior = repo.sugerencias.first { it.id == "b-viejo" }
        assertEquals(false, anterior.aceptada)
        assertEquals("demo-lramirez", anterior.decididaPorId)
        assertNull(repo.sugerencias.first { it.id == nuevo.id }.aceptada)
    }

    @Test
    fun `guarda el borrador sin decidir con lo que se envio`() = runTest {
        // RF-1417, RF-1419
        comoLuis()

        val sugerencia = (redactar(deLuis, entrada, ahora) as Resultado.Exito).valor

        assertEquals(listOf(entrada), asistente.entradasDeBorrador)
        assertEquals(TipoSugerencia.BORRADOR_INFORME, sugerencia.tipo)
        assertEquals("modelo-de-prueba", sugerencia.modelo)
        assertEquals(ahora, sugerencia.generadaEn)
        assertEquals("Fuente dañada por voltaje", leerBorrador(sugerencia.contenido)?.diagnostico)
        assertEquals(listOf(sugerencia), repo.sugerencias)
    }
}
