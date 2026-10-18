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

/** HU-09 (RF-925, RF-926, RF-928, RF-929): enlazar la foto ya tomada, o borrarla si no se puede. */
class GuardarEvidenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = EvidenciaRepositoryFalso()
    private val guardar = GuardarEvidencia(VerificarPermiso(sesion), repo)
    private val ahora = Instant.parse("2026-10-18T15:30:00Z")
    private val foto = FotoNueva("f1", "/evidencias/i124/f1.jpg")

    // INC-0124: asignada a Luis Ramírez, en atención
    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun fotosPrevias(cantidad: Int) = List(cantidad) { i ->
        Evidencia("p$i", deLuis.id, TipoEvidencia.FALLA, "/evidencias/i124/p$i.jpg", ahora.minusSeconds(60L * (i + 1)))
    }

    private fun assertDescartadaSinEnlazar() {
        assertEquals(listOf(foto.ruta), repo.descartadas)
        assertTrue(repo.evidencias.none { it.id == foto.id })
    }

    @Test
    fun `sin sesion no enlaza y borra el archivo`() = runTest {
        repo.cargar(incidencias = listOf(deLuis))

        val r = guardar(deLuis.id, foto, TipoEvidencia.FALLA, ahora)

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), r)
        assertDescartadaSinEnlazar()
    }

    @Test
    fun `otro tecnico no enlaza y borra el archivo`() = runTest {
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis))

        val r = guardar(deLuis.id, foto, TipoEvidencia.FALLA, ahora)

        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), r)
        assertDescartadaSinEnlazar()
    }

    @Test
    fun `cerrada mientras se tomaba la foto no enlaza y borra el archivo`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis.copy(marca = MarcaSeguimiento.CERRADA)))

        val r = guardar(deLuis.id, foto, TipoEvidencia.FALLA, ahora)

        assertEquals(Resultado.Error(ErrorDominio.IncidenciaCerrada), r)
        assertDescartadaSinEnlazar()
    }

    @Test
    fun `con 10 fotos no enlaza la undecima y borra el archivo`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis), evidencias = fotosPrevias(MAX_FOTOS))

        val r = guardar(deLuis.id, foto, TipoEvidencia.FALLA, ahora)

        assertEquals(Resultado.Error(ErrorDominio.LimiteFotosAlcanzado), r)
        assertDescartadaSinEnlazar()
        assertEquals(MAX_FOTOS, repo.contarEvidencias(deLuis.id))
    }

    @Test
    fun `el tecnico asignado enlaza la foto con su tipo, su ruta y la hora recibida`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis), evidencias = fotosPrevias(MAX_FOTOS - 1))

        val r = guardar(deLuis.id, foto, TipoEvidencia.PIEZA_CAMBIADA, ahora)

        val esperada = Evidencia(foto.id, deLuis.id, TipoEvidencia.PIEZA_CAMBIADA, foto.ruta, fechaCaptura = ahora)
        assertEquals(Resultado.Exito(esperada), r)
        assertEquals(esperada, repo.evidencias.single { it.id == foto.id })
        assertTrue(repo.descartadas.isEmpty())
    }

    @Test
    fun `si el repositorio falla devuelve el error y no queda nada enlazado`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis))
        repo.fallar = true

        val r = guardar(deLuis.id, foto, TipoEvidencia.FALLA, ahora)

        assertTrue(r is Resultado.Error && r.causa is ErrorDominio.Desconocido)
        assertDescartadaSinEnlazar()   // el repositorio borró el archivo al fallar
    }
}
