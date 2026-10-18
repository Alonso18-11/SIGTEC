package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-09 (RF-924, RF-929, RF-931): la captura se bloquea sola cuando cambia la incidencia o llega al máximo. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObservarCapturaEvidenciaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = EvidenciaRepositoryFalso()
    private val observar = ObservarCapturaEvidencia(repo, ObservarUsuarioActual(sesion))
    private val ahora = Instant.parse("2026-10-18T15:30:00Z")

    // INC-0124: asignada a Luis Ramírez, en atención
    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun fotos(cantidad: Int) = List(cantidad) { i ->
        Evidencia("p$i", deLuis.id, TipoEvidencia.FALLA, "/evidencias/i124/p$i.jpg", ahora.plusSeconds(i.toLong()))
    }

    @Test
    fun `el tecnico asignado ve la incidencia y sus fotos sin bloqueo`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis), evidencias = fotos(2))

        val captura = observar(deLuis.id).first()

        assertEquals(deLuis, captura?.incidencia)
        assertEquals(fotos(2), captura?.fotos)
        assertNull(captura?.bloqueo)
    }

    @Test
    fun `sin sesion queda bloqueada`() = runTest {
        repo.cargar(incidencias = listOf(deLuis))

        assertEquals(ErrorDominio.SinPermiso, observar(deLuis.id).first()?.bloqueo)
    }

    @Test
    fun `al llegar a 10 fotos se bloquea sola`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis), evidencias = fotos(MAX_FOTOS - 1))
        val vistos = mutableListOf<CapturaEvidencia?>()
        observar(deLuis.id).onEach { vistos += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        repo.cargar(evidencias = fotos(MAX_FOTOS))

        assertNull(vistos.first()?.bloqueo)
        assertEquals(ErrorDominio.LimiteFotosAlcanzado, vistos.last()?.bloqueo)
    }

    @Test
    fun `reasignada o cerrada desde otro celular se bloquea sola`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis), evidencias = fotos(1))
        val vistos = mutableListOf<CapturaEvidencia?>()
        observar(deLuis.id).onEach { vistos += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        repo.cargar(incidencias = listOf(deLuis.copy(tecnicoAsignadoId = "demo-atorres", tecnicoAsignadoNombre = "Ana Torres")))
        assertEquals(ErrorDominio.SinPermiso, vistos.last()?.bloqueo)

        repo.cargar(incidencias = listOf(deLuis.copy(marca = MarcaSeguimiento.CERRADA)))
        assertEquals(ErrorDominio.IncidenciaCerrada, vistos.last()?.bloqueo)
        assertEquals(fotos(1), vistos.last()?.fotos)   // las fotos ya tomadas se conservan
    }

    @Test
    fun `una incidencia que no esta en el celular da null`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")

        assertNull(observar("no-existe").first())
    }
}
