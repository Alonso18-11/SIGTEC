package com.softcorp.sigtec.feature.asignacion.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.asignacion.data.AsignacionRepositoryFalso
import com.softcorp.sigtec.feature.asignacion.domain.AsignarIncidencia
import com.softcorp.sigtec.feature.asignacion.domain.ObservarOpcionesAsignacion
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.ia.data.AsistenteIAFalso
import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryFalso
import com.softcorp.sigtec.feature.ia.domain.ClasificacionSugerida
import com.softcorp.sigtec.feature.ia.domain.DecidirSugerencia
import com.softcorp.sigtec.feature.ia.domain.ObservarSugerencia
import com.softcorp.sigtec.feature.ia.domain.SugerirClasificacion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-06 (RF-605, RF-607, RF-612, RF-616, RF-617): estado y eventos de la pantalla de asignación. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AsignacionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = AsignacionRepositoryFalso()
    private val asistente = AsistenteIAFalso()
    private val sugerencias = SugerenciaIARepositoryFalso()

    private val luis = Tecnico("demo-lramirez", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3)
    private val ana = Tecnico("demo-atorres", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 5)

    private val incidencia = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    private suspend fun TestScope.nuevoVm(inc: Incidencia = incidencia): AsignacionViewModel {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        repo.cargar(listOf(inc), listOf(luis, ana))
        val vm = AsignacionViewModel(
            SavedStateHandle(mapOf(AsignacionViewModel.ARG_INCIDENCIA_ID to inc.id)),
            ObservarOpcionesAsignacion(repo),
            AsignarIncidencia(VerificarPermiso(sesion), repo),
            SugerirClasificacion(VerificarPermiso(sesion), asistente, sugerencias),
            ObservarSugerencia(sugerencias),
            DecidirSugerencia(VerificarPermiso(sesion), sugerencias)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `sin eleccion no se puede asignar y al elegir uno con cupo si`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.estado.value.puedeAsignar)

        vm.alElegir(luis.id)

        assertEquals("Luis Ramírez", vm.estado.value.elegido?.tecnico?.nombre)
        assertTrue(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `un tecnico sin cupo no se puede elegir`() = runTest {
        // RF-603: Ana está en 5 de 5
        val vm = nuevoVm()
        vm.alElegir(ana.id)
        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `por sincronizar muestra el motivo y no deja asignar`() = runTest {
        // RF-607, RF-610
        val vm = nuevoVm(incidencia.copy(numero = null))
        vm.alElegir(luis.id)
        assertEquals(ErrorDominio.IncidenciaPorSincronizar, vm.estado.value.opciones?.bloqueo)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `al asignar emite el evento una sola vez y no permite guardar dos veces`() = runTest {
        // RF-617 y sin registro doble
        val vm = nuevoVm()
        vm.alElegir(luis.id)

        vm.alAsignar()
        vm.alAsignar()

        assertEquals("Luis Ramírez", vm.estado.value.asignadaA)
        assertTrue(vm.estado.value.guardando)
        assertEquals(1, repo.movimientos.size)
        vm.alNavegarAlDetalle()
        assertNull(vm.estado.value.asignadaA)
    }

    @Test
    fun `si el elegido se queda sin cupo la eleccion se borra`() = runTest {
        // RF-602: otra asignación suya, guardada en este celular, todavía no subió
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        repo.fijarSinSubir(mapOf(luis.id to 2))
        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `si Room falla muestra el error y se puede reintentar`() = runTest {
        // RF-616
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        repo.fallar = true

        vm.alAsignar()

        assertTrue(vm.estado.value.error is ErrorDominio.Desconocido)
        assertFalse(vm.estado.value.guardando)
        assertNull(vm.estado.value.asignadaA)
        assertTrue(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `con tecnico actual es reasignacion y el actual no se puede elegir`() = runTest {
        // RF-604, RF-605
        val asignadaALuis = incidencia.copy(
            marca = MarcaSeguimiento.ASIGNADA, tecnicoAsignadoId = luis.id, tecnicoAsignadoNombre = luis.nombre
        )
        val vm = nuevoVm(asignadaALuis)
        assertTrue(vm.estado.value.esReasignacion)

        vm.alElegir(luis.id)

        assertNull(vm.estado.value.elegido)
        assertFalse(vm.estado.value.puedeAsignar)
    }

    @Test
    fun `ahora avanza cada minuto sin que cambien la eleccion ni las opciones`() = runTest {
        // Caso límite de la spec (T47): los días pendientes del resumen no quedan desactualizados
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        val primera = vm.estado.value.ahora
        val opcionesAntes = vm.estado.value.opciones
        val elegidoAntes = vm.estado.value.elegido

        // Igual que en DetalleIncidenciaViewModelTest: el reloj usa Instant.now() real, así que una
        // espera breve asegura que avance; luego se adelanta el scheduler de Main un minuto.
        Thread.sleep(50)
        main.dispatcher.scheduler.advanceTimeBy(60_000L)
        main.dispatcher.scheduler.runCurrent()

        assertNotEquals(primera, vm.estado.value.ahora)
        assertEquals(opcionesAntes, vm.estado.value.opciones)
        assertEquals(elegidoAntes, vm.estado.value.elegido)
    }

    @Test
    fun `empieza con la clasificacion de la incidencia y la conserva al asignar`() = runTest {
        // HU-14 (RF-1408)
        val vm = nuevoVm(incidencia.copy(tipoFalla = TipoFalla.HARDWARE, prioridad = Prioridad.ALTA))
        assertEquals(TipoFalla.HARDWARE, vm.estado.value.tipoElegido)
        assertEquals(Prioridad.ALTA, vm.estado.value.prioridadElegida)

        vm.alElegir(luis.id)
        vm.alAsignar()

        assertEquals(TipoFalla.HARDWARE, repo.incidencias.single().tipoFalla)
        assertEquals(Prioridad.ALTA, repo.incidencias.single().prioridad)
    }

    @Test
    fun `la clasificacion elegida se guarda al asignar`() = runTest {
        // HU-14 (RF-1411, RF-1412)
        val vm = nuevoVm()
        assertNull(vm.estado.value.tipoElegido)

        vm.alElegirTipo(TipoFalla.RED)
        vm.alElegirPrioridad(Prioridad.MEDIA)
        vm.alElegir(luis.id)
        vm.alAsignar()

        assertEquals(TipoFalla.RED, repo.incidencias.single().tipoFalla)
        assertEquals(Prioridad.MEDIA, repo.incidencias.single().prioridad)
    }

    @Test
    fun `clasificar es opcional`() = runTest {
        // HU-14 (D-3): sin tipo ni prioridad igual se asigna
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        assertTrue(vm.estado.value.puedeAsignar)

        vm.alAsignar()

        assertEquals(luis.id, repo.incidencias.single().tecnicoAsignadoId)
        assertNull(repo.incidencias.single().tipoFalla)
    }

    private fun decision() = sugerencias.sugerencias.single { it.tipo == TipoSugerencia.CLASIFICACION }.aceptada

    @Test
    fun `sin clasificar pide la sugerencia y la muestra sin elegirla`() = runTest {
        // HU-14 (RF-1401, RF-1404, RF-1408)
        val vm = nuevoVm()

        assertEquals(listOf(incidencia.descripcion), asistente.descripcionesClasificadas)
        assertEquals(ClasificacionSugerida(TipoFalla.RED, Prioridad.MEDIA), vm.estado.value.clasificacionSugerida)
        assertTrue(vm.estado.value.tarjetaSugerenciaVisible)
        assertFalse(vm.estado.value.generandoSugerencia)
        assertNull(vm.estado.value.tipoElegido)
    }

    @Test
    fun `una incidencia ya clasificada no pide sugerencia`() = runTest {
        // HU-14 (RF-1407)
        val vm = nuevoVm(incidencia.copy(tipoFalla = TipoFalla.SOFTWARE))
        assertTrue(asistente.descripcionesClasificadas.isEmpty())
        assertFalse(vm.estado.value.tarjetaSugerenciaVisible)
    }

    @Test
    fun `si Gemini falla no hay tarjeta ni error y se asigna igual`() = runTest {
        // HU-14 (RF-1406)
        asistente.respuestaClasificacion = Resultado.Error(ErrorDominio.IANoDisponible)
        val vm = nuevoVm()

        assertFalse(vm.estado.value.tarjetaSugerenciaVisible)
        assertNull(vm.estado.value.error)
        vm.alElegir(luis.id)
        vm.alAsignar()
        assertEquals(luis.id, repo.incidencias.single().tecnicoAsignadoId)
    }

    @Test
    fun `usar la sugerencia la pone en clasificacion y oculta la tarjeta`() = runTest {
        // HU-14 (RF-1409)
        val vm = nuevoVm()
        vm.alUsarSugerencia()

        assertEquals(TipoFalla.RED, vm.estado.value.tipoElegido)
        assertEquals(Prioridad.MEDIA, vm.estado.value.prioridadElegida)
        assertFalse(vm.estado.value.tarjetaSugerenciaVisible)
    }

    @Test
    fun `usada y asignada tal cual queda aceptada`() = runTest {
        // HU-14 (RF-1412, RF-1413)
        val vm = nuevoVm()
        vm.alUsarSugerencia()
        vm.alElegir(luis.id)
        vm.alAsignar()

        assertEquals(TipoFalla.RED, repo.incidencias.single().tipoFalla)
        assertEquals(Prioridad.MEDIA, repo.incidencias.single().prioridad)
        assertEquals(true, decision())
        assertEquals("demo-cmendoza", sugerencias.sugerencias.single().decididaPorId)
    }

    @Test
    fun `usada y corregida queda no aceptada con la correccion guardada`() = runTest {
        // HU-14 (RF-1409, RF-1413)
        val vm = nuevoVm()
        vm.alUsarSugerencia()
        vm.alElegirPrioridad(Prioridad.ALTA)
        vm.alElegir(luis.id)
        vm.alAsignar()

        assertEquals(Prioridad.ALTA, repo.incidencias.single().prioridad)
        assertEquals(false, decision())
    }

    @Test
    fun `descartada queda no aceptada y no cambia la clasificacion`() = runTest {
        // HU-14 (RF-1410, RF-1413)
        val vm = nuevoVm()
        vm.alDescartarSugerencia()
        assertFalse(vm.estado.value.tarjetaSugerenciaVisible)
        vm.alElegir(luis.id)
        vm.alAsignar()

        assertNull(repo.incidencias.single().tipoFalla)
        assertEquals(false, decision())
    }

    @Test
    fun `asignar sin usarla la deja no aceptada`() = runTest {
        // HU-14 (RF-1413)
        val vm = nuevoVm()
        vm.alElegir(luis.id)
        vm.alAsignar()
        assertEquals(false, decision())
    }

    @Test
    fun `si la asignacion falla la sugerencia sigue sin decidir`() = runTest {
        // HU-14 (RF-1412)
        val vm = nuevoVm()
        vm.alUsarSugerencia()
        vm.alElegir(luis.id)
        repo.fallar = true
        vm.alAsignar()

        assertNull(decision())
        assertNotEquals(null, vm.estado.value.error)
    }
}
