package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.indicadores.data.IndicadoresRepositoryFalso
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** HU-18 (RF-1812, RF-1816): los números de los avisos del jefe salen del mismo cálculo del tablero. */
class CalcularIndicadoresDeHoyTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = IndicadoresRepositoryFalso()
    private val zona = ZoneId.of("America/Lima")
    private val ahora = Instant.parse("2026-11-16T23:30:00Z")   // 18:30 en Lima

    private fun incidencia(id: String, registro: String, marca: MarcaSeguimiento) = Incidencia(
        id = id, numero = null, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse(registro), registradoPorId = "u",
        registradoPorNombre = "Carlos Mendoza", marca = marca
    )

    private fun cierre(incidenciaId: String, fecha: String) = Movimiento(
        id = "m$incidenciaId", incidenciaId = incidenciaId, tipo = TipoMovimiento.CERRADA,
        autorNombre = "Luis Ramírez", fecha = Instant.parse(fecha)
    )

    @Test
    fun `sin sesion o sin VER_INDICADORES devuelve sin permiso`() = runTest {
        val calcular = CalcularIndicadoresDeHoy(VerificarPermiso(sesion), repo, StandardTestDispatcher(testScheduler))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), calcular(ahora, zona))
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), calcular(ahora, zona))
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), calcular(ahora, zona))
    }

    @Test
    fun `el jefe recibe los reparados de hoy, los pendientes y los criticos`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.datos.value = repo.datos.value.copy(
            incidencias = listOf(
                incidencia("hoy", "2026-11-15T14:00:00Z", MarcaSeguimiento.CERRADA),
                incidencia("ayer", "2026-11-14T14:00:00Z", MarcaSeguimiento.CERRADA),
                incidencia("demorada", "2026-11-05T14:00:00Z", MarcaSeguimiento.EN_ATENCION),
                incidencia("nueva", "2026-11-16T14:00:00Z", MarcaSeguimiento.SIN_ASIGNAR)
            ),
            cierres = listOf(
                cierre("hoy", "2026-11-16T16:00:00Z"),    // 11:00 en Lima: hoy
                cierre("ayer", "2026-11-16T04:00:00Z")    // 23:00 del día 15 en Lima: ayer
            )
        )
        val calcular = CalcularIndicadoresDeHoy(VerificarPermiso(sesion), repo, StandardTestDispatcher(testScheduler))

        val indicadores = (calcular(ahora, zona) as Resultado.Exito).valor

        assertEquals(1, indicadores.reparadosHoy)
        assertEquals(2, indicadores.pendientes)
        assertEquals(1, indicadores.criticos)
    }
}
