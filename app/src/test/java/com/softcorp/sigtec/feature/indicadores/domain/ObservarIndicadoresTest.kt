package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.indicadores.data.IndicadoresRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** HU-15 (RF-1503, RF-1504): solo el jefe ve los indicadores. */
class ObservarIndicadoresTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = IndicadoresRepositoryFalso()
    private val ahora = Instant.parse("2026-11-09T20:00:00Z")

    private fun crear(dispatcher: TestDispatcher) =
        ObservarIndicadores(VerificarPermiso(sesion), repo, dispatcher)

    @Test
    fun `sin sesion y con un tecnico devuelve sin permiso`() = runTest {
        val observar = crear(StandardTestDispatcher(testScheduler))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), observar(flowOf(ahora)))
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), observar(flowOf(ahora)))
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), observar(flowOf(ahora)))
    }

    @Test
    fun `el jefe recibe los indicadores calculados y se recalculan con datos nuevos`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val flujo = (crear(StandardTestDispatcher(testScheduler))(flowOf(ahora), ZoneId.of("America/Lima"))
            as Resultado.Exito).valor
        assertEquals(0, flujo.first().pendientes)

        repo.datos.value = repo.datos.value.copy(
            incidencias = listOf(
                Incidencia(
                    id = "i1", numero = 1, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
                    usuarioResponsable = "María Quispe", fechaRegistro = ahora, registradoPorId = "u",
                    registradoPorNombre = "Carlos Mendoza", marca = MarcaSeguimiento.SIN_ASIGNAR
                )
            )
        )
        assertEquals(1, flujo.first().pendientes)
    }
}
