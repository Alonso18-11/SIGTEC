package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.indicadores.data.HistorialRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-16 (RF-1602, RF-1604): los tres perfiles consultan el historial; sin sesión, no. */
class ObservarHistorialEquipoTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = HistorialRepositoryFalso()
    private val ahora = Instant.parse("2026-11-16T15:00:00Z")

    private fun TestScope.crear() = ObservarHistorialEquipo(VerificarPermiso(sesion), repo, StandardTestDispatcher(testScheduler))

    private fun incidencia(id: String, equipo: String = "PC-CONT-014") = Incidencia(
        id = id, numero = 1, codigoEquipo = equipo, descripcion = "No enciende", usuarioResponsable = "María Quispe",
        fechaRegistro = ahora, registradoPorId = "u", registradoPorNombre = "Carlos Mendoza",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `sin sesion devuelve sin permiso y no lee nada`() = runTest {
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), crear()("PC-CONT-014", flowOf(ahora)))
        assertTrue(repo.codigosPedidos.isEmpty())
    }

    @Test
    fun `el jefe el tecnico y el personal de sistemas lo consultan`() = runTest {
        for (correo in listOf("cmendoza@softcorp.pe", "lramirez@softcorp.pe", "pcardenas@softcorp.pe")) {
            sesion.iniciarSesion(correo, "")
            assertTrue(correo, crear()("PC-CONT-014", flowOf(ahora)) is Resultado.Exito)
        }
    }

    @Test
    fun `el codigo se busca sin espacios y en mayusculas`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val flujo = (crear()("  pc-cont-014 ", flowOf(ahora)) as Resultado.Exito).valor

        assertEquals("PC-CONT-014", flujo.first().codigo)
        assertEquals(listOf("PC-CONT-014"), repo.codigosPedidos)
    }

    @Test
    fun `un codigo en blanco es un campo obligatorio`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(Resultado.Error(ErrorDominio.CampoObligatorio), crear()("   ", flowOf(ahora)))
    }

    @Test
    fun `se recalcula cuando llega una incidencia nueva del equipo`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val flujo = (crear()("PC-CONT-014", flowOf(ahora)) as Resultado.Exito).valor
        assertEquals(0, flujo.first().atenciones.size)

        repo.datos.value = repo.datos.value.copy(incidencias = listOf(incidencia("i1"), incidencia("i2", "PC-LOG-009")))

        assertEquals(listOf("i1"), flujo.first().atenciones.map { it.incidenciaId })
    }
}
