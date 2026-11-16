package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoFalla
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

/** HU-14 (RF-1401 a RF-1407): la clasificación se pide una vez, solo para el jefe y solo si hace falta. */
class SugerirClasificacionTest {

    private val sesion = SesionRepositoryDemo()
    private val asistente = AsistenteIAFalso()
    private val repo = SugerenciaIARepositoryFalso()
    private val sugerir = SugerirClasificacion(VerificarPermiso(sesion), asistente, repo)
    private val ahora = Instant.parse("2026-11-16T15:00:00Z")

    private val sinClasificar = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-16T10:00:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    private suspend fun comoJefe() = sesion.iniciarSesion("cmendoza@softcorp.pe", "")

    @Test
    fun `sin sesion no llama a Gemini`() = runTest {
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), sugerir(sinClasificar, ahora))
        assertTrue(asistente.descripcionesClasificadas.isEmpty())
    }

    @Test
    fun `el personal de sistemas no puede pedirla`() = runTest {
        // RF-1402
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), sugerir(sinClasificar, ahora))
        assertTrue(asistente.descripcionesClasificadas.isEmpty())
    }

    @Test
    fun `una incidencia ya clasificada o cerrada no la pide`() = runTest {
        // RF-1407
        comoJefe()
        assertEquals(Resultado.Exito(null), sugerir(sinClasificar.copy(prioridad = Prioridad.ALTA), ahora))
        assertEquals(Resultado.Exito(null), sugerir(sinClasificar.copy(tipoFalla = TipoFalla.RED), ahora))
        assertEquals(Resultado.Exito(null), sugerir(sinClasificar.copy(marca = MarcaSeguimiento.CERRADA), ahora))
        assertTrue(asistente.descripcionesClasificadas.isEmpty())
    }

    @Test
    fun `si ya se pidio en este celular no la vuelve a pedir`() = runTest {
        // RF-1405: aunque ya esté decidida
        comoJefe()
        repo.cargar(
            SugerenciaIA("s1", "i127", TipoSugerencia.CLASIFICACION, "{}", "m", ahora, aceptada = false)
        )
        assertEquals(Resultado.Exito(null), sugerir(sinClasificar, ahora))
        assertTrue(asistente.descripcionesClasificadas.isEmpty())
    }

    @Test
    fun `si Gemini falla devuelve el error y no guarda nada`() = runTest {
        // RF-1406
        comoJefe()
        asistente.respuestaClasificacion = Resultado.Error(ErrorDominio.IANoDisponible)
        assertEquals(Resultado.Error(ErrorDominio.IANoDisponible), sugerir(sinClasificar, ahora))
        assertTrue(repo.sugerencias.isEmpty())
    }

    @Test
    fun `una respuesta que no sirve no se guarda`() = runTest {
        // RF-1406
        comoJefe()
        asistente.respuestaClasificacion = Resultado.Exito("""{"tipo": "ELECTRICO", "prioridad": "ALTA"}""")
        assertEquals(Resultado.Error(ErrorDominio.IANoDisponible), sugerir(sinClasificar, ahora))
        assertTrue(repo.sugerencias.isEmpty())
    }

    @Test
    fun `guarda la sugerencia sin decidir con el modelo y la fecha`() = runTest {
        // RF-1401, RF-1404, RF-1427
        comoJefe()

        val sugerencia = (sugerir(sinClasificar, ahora) as Resultado.Exito).valor!!

        assertEquals(listOf("La impresora no aparece en la red"), asistente.descripcionesClasificadas)
        assertEquals(TipoSugerencia.CLASIFICACION, sugerencia.tipo)
        assertEquals("i127", sugerencia.incidenciaId)
        assertEquals("modelo-de-prueba", sugerencia.modelo)
        assertEquals(ahora, sugerencia.generadaEn)
        assertNull(sugerencia.aceptada)
        assertEquals(ClasificacionSugerida(TipoFalla.RED, Prioridad.MEDIA), leerClasificacion(sugerencia.contenido))
        assertEquals(listOf(sugerencia), repo.sugerencias)
    }

    @Test
    fun `si Room falla devuelve el error`() = runTest {
        comoJefe()
        repo.fallar = true
        assertTrue((sugerir(sinClasificar, ahora) as Resultado.Error).causa is ErrorDominio.Desconocido)
    }
}
