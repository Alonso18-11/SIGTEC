package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-09 (RF-937, D-5): solo el técnico asignado elimina, y solo mientras la incidencia siga pendiente. */
class EliminarEvidenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = EvidenciaRepositoryFalso()
    private val eliminar = EliminarEvidencia(VerificarPermiso(sesion), repo)

    // INC-0124: asignada a Luis Ramírez, en atención
    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )
    private val foto = Evidencia(
        "f1", deLuis.id, TipoEvidencia.FALLA, "/evidencias/i124/f1.jpg", Instant.parse("2026-10-18T15:00:00Z")
    )

    private fun assertSinEliminar() {
        assertEquals(listOf(foto), repo.evidencias)
        assertTrue(repo.descartadas.isEmpty())
    }

    @Test
    fun `sin sesion no elimina`() = runTest {
        repo.cargar(listOf(deLuis), listOf(foto))

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), eliminar(foto))
        assertSinEliminar()
    }

    @Test
    fun `otro tecnico no elimina`() = runTest {
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        repo.cargar(listOf(deLuis), listOf(foto))

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), eliminar(foto))
        assertSinEliminar()
    }

    @Test
    fun `el jefe no elimina`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(listOf(deLuis), listOf(foto))

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), eliminar(foto))
        assertSinEliminar()
    }

    @Test
    fun `con la incidencia cerrada la foto queda como respaldo`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(deLuis.copy(marca = MarcaSeguimiento.CERRADA)), listOf(foto))

        assertEquals(Resultado.Error(ErrorDominio.IncidenciaCerrada), eliminar(foto))
        assertSinEliminar()
    }

    @Test
    fun `el tecnico asignado elimina la foto y su archivo`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(deLuis), listOf(foto))

        assertEquals(Resultado.Exito(Unit), eliminar(foto))
        assertTrue(repo.evidencias.isEmpty())
        assertEquals(listOf(foto.rutaArchivo), repo.descartadas)
    }

    @Test
    fun `con el maximo de fotos tambien se puede eliminar`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val diez = List(MAX_FOTOS) { i -> foto.copy(id = "f$i", rutaArchivo = "/evidencias/i124/f$i.jpg") }
        repo.cargar(listOf(deLuis), diez)

        assertEquals(Resultado.Exito(Unit), eliminar(diez.first()))
        assertEquals(MAX_FOTOS - 1, repo.evidencias.size)
    }

    @Test
    fun `si Room falla el error llega a quien llama y la foto sigue`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(deLuis), listOf(foto))
        repo.fallar = true

        assertTrue(eliminar(foto) is Resultado.Error)
        assertSinEliminar()
    }
}
