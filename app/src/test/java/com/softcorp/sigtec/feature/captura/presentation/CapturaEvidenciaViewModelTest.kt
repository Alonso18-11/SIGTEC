package com.softcorp.sigtec.feature.captura.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import com.softcorp.sigtec.feature.captura.domain.DescartarFoto
import com.softcorp.sigtec.feature.captura.domain.GuardarEvidencia
import com.softcorp.sigtec.feature.captura.domain.MAX_FOTOS
import com.softcorp.sigtec.feature.captura.domain.ObservarCapturaEvidencia
import com.softcorp.sigtec.feature.captura.domain.PrepararFoto
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-09 (RF-924 a RF-931): el selector, el disparo, el guardado, la falla y el máximo. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CapturaEvidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = EvidenciaRepositoryFalso()

    // INC-0124: asignada a Luis Ramírez, en atención
    private val deLuis = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun TestScope.nuevoVm(guardado: Map<String, Any?> = emptyMap()): CapturaEvidenciaViewModel {
        val vm = CapturaEvidenciaViewModel(
            SavedStateHandle(mapOf(CapturaEvidenciaViewModel.ARG_INCIDENCIA_ID to deLuis.id) + guardado),
            ObservarCapturaEvidencia(repo, ObservarUsuarioActual(sesion)),
            PrepararFoto(repo),
            GuardarEvidencia(VerificarPermiso(sesion), repo),
            DescartarFoto(repo)
        )
        // WhileSubscribed necesita a alguien mirando; Unconfined lo pone a mirar de inmediato
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val CapturaEvidenciaViewModel.ui: CapturaEvidenciaUiState get() = estado.value

    private suspend fun comoLuis() {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(incidencias = listOf(deLuis))
    }

    @Test
    fun `empieza en falla y cambia a pieza cambiada`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        assertEquals(TipoEvidencia.FALLA, vm.ui.tipo)

        vm.alCambiarTipo(TipoEvidencia.PIEZA_CAMBIADA)

        assertEquals(TipoEvidencia.PIEZA_CAMBIADA, vm.ui.tipo)
    }

    @Test
    fun `el tipo elegido sobrevive a la rotacion`() = runTest {
        comoLuis()
        val vm = nuevoVm(guardado = mapOf("tipo" to TipoEvidencia.PIEZA_CAMBIADA))

        assertEquals(TipoEvidencia.PIEZA_CAMBIADA, vm.ui.tipo)
    }

    @Test
    fun `disparar deja la foto para la camara y deshabilita el disparador`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        assertTrue(vm.ui.puedeDisparar)

        vm.alDisparar()

        assertNotNull(vm.ui.fotoPorTomar)
        assertTrue(vm.ui.guardando)
        assertFalse(vm.ui.puedeDisparar)   // RF-927
    }

    @Test
    fun `un segundo toque mientras guarda no prepara otra foto`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        vm.alDisparar()
        val primera = vm.ui.fotoPorTomar
        vm.alFotoEnCamara()

        vm.alDisparar()

        assertNull(vm.ui.fotoPorTomar)
        assertNotNull(primera)
    }

    @Test
    fun `la foto tomada se guarda con el tipo elegido y vuelve a habilitar el disparador`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        vm.alCambiarTipo(TipoEvidencia.PIEZA_CAMBIADA)
        vm.alDisparar()
        val foto = vm.ui.fotoPorTomar!!
        vm.alFotoEnCamara()

        vm.alFotoTomada(foto)

        val guardada = repo.evidencias.single()
        assertEquals(foto.id, guardada.id)
        assertEquals(TipoEvidencia.PIEZA_CAMBIADA, guardada.tipo)
        assertEquals(listOf(guardada), vm.ui.fotos)   // RF-925: aparece en la fila
        assertFalse(vm.ui.guardando)
        assertNull(vm.ui.error)
        assertTrue(vm.ui.puedeDisparar)
    }

    @Test
    fun `si la camara falla descarta el archivo, avisa y deja volver a disparar`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        vm.alDisparar()
        val foto = vm.ui.fotoPorTomar!!
        vm.alFotoEnCamara()

        vm.alFalloCaptura(foto)

        assertEquals(listOf(foto.ruta), repo.descartadas)   // RF-928
        assertTrue(repo.evidencias.isEmpty())
        assertTrue(vm.ui.error is ErrorDominio.Desconocido)
        assertTrue(vm.ui.puedeDisparar)
    }

    @Test
    fun `con 10 fotos el disparador queda deshabilitado y disparar no hace nada`() = runTest {
        comoLuis()
        repo.cargar(evidencias = List(MAX_FOTOS) { i ->
            Evidencia("p$i", deLuis.id, TipoEvidencia.FALLA, "/evidencias/i124/p$i.jpg", Instant.EPOCH.plusSeconds(i.toLong()))
        })
        val vm = nuevoVm()

        vm.alDisparar()

        assertEquals(ErrorDominio.LimiteFotosAlcanzado, vm.ui.bloqueo)   // RF-929
        assertFalse(vm.ui.puedeDisparar)
        assertNull(vm.ui.fotoPorTomar)
    }

    @Test
    fun `si la cierran mientras se toma la foto, no la enlaza y avisa`() = runTest {
        comoLuis()
        val vm = nuevoVm()
        vm.alDisparar()
        val foto = vm.ui.fotoPorTomar!!
        vm.alFotoEnCamara()
        repo.cargar(incidencias = listOf(deLuis.copy(marca = MarcaSeguimiento.CERRADA)))   // llega desde otro celular

        vm.alFotoTomada(foto)

        assertTrue(repo.evidencias.isEmpty())
        assertEquals(listOf(foto.ruta), repo.descartadas)
        assertEquals(ErrorDominio.IncidenciaCerrada, vm.ui.bloqueo)   // RF-931
        assertFalse(vm.ui.puedeDisparar)
    }
}
