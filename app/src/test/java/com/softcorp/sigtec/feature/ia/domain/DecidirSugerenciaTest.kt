package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-14 (RF-1413, RF-1420, RF-1421): la decisión, con quién y cuándo, y el permiso de cada tipo. */
class DecidirSugerenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = SugerenciaIARepositoryFalso()
    private val decidir = DecidirSugerencia(VerificarPermiso(sesion), repo)
    private val generada = Instant.parse("2026-11-16T15:00:00Z")
    private val ahora = Instant.parse("2026-11-16T15:02:00Z")

    private val clasificacion = SugerenciaIA("c1", "i127", TipoSugerencia.CLASIFICACION, "{}", "m", generada)
    private val borrador = SugerenciaIA("b1", "i124", TipoSugerencia.BORRADOR_INFORME, "{}", "m", generada)

    @Test
    fun `el jefe decide la clasificacion con quien y cuando`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(clasificacion)

        assertEquals(Resultado.Exito(Unit), decidir(clasificacion, aceptada = true, ahora = ahora))

        val decidida = repo.sugerencias.single()
        assertEquals(true, decidida.aceptada)
        assertEquals("demo-cmendoza", decidida.decididaPorId)
        assertEquals(ahora, decidida.decididaEn)
    }

    @Test
    fun `un tecnico no decide la clasificacion`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(clasificacion)

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), decidir(clasificacion, aceptada = true, ahora = ahora))
        assertNull(repo.sugerencias.single().aceptada)
    }

    @Test
    fun `el tecnico decide el borrador y el jefe no`() = runTest {
        repo.cargar(borrador)
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), decidir(borrador, aceptada = false, ahora = ahora))

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(Resultado.Exito(Unit), decidir(borrador, aceptada = false, ahora = ahora))
        assertEquals(false, repo.sugerencias.single().aceptada)
    }

    @Test
    fun `una sugerencia ya decidida no cambia`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val yaDecidida = clasificacion.copy(aceptada = false, decididaPorId = "demo-cmendoza", decididaEn = generada)
        repo.cargar(yaDecidida)

        assertEquals(Resultado.Exito(Unit), decidir(yaDecidida, aceptada = true, ahora = ahora))
        assertEquals(yaDecidida, repo.sugerencias.single())
    }
}
