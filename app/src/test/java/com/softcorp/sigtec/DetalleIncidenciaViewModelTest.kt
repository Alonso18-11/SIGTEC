package com.softcorp.sigtec

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.IniciarAtencion
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import com.softcorp.sigtec.feature.incidencias.presentation.DetalleIncidenciaViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetalleIncidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val t0 = Instant.parse("2026-09-27T14:15:00Z")

    // Asignada a Luis y en atención, como INC-0124 del prototipo
    private val incidencia = Incidencia(
        id = "c", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = t0, registradoPorId = "demo-lramirez",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    // Cargados desordenados: el detalle debe mostrarlos del más reciente al más antiguo
    private val movimientos = listOf(
        Movimiento("m1", "c", TipoMovimiento.REGISTRADA, "Luis Ramírez", t0),
        Movimiento("m3", "c", TipoMovimiento.ATENCION_INICIADA, "Luis Ramírez", t0.plusSeconds(35 * 60)),
        Movimiento("m2", "c", TipoMovimiento.ASIGNADA, "Carlos Mendoza", t0.plusSeconds(17 * 60)),
        Movimiento("x", "otra", TipoMovimiento.REGISTRADA, "Ana Torres", t0.plusSeconds(60 * 60))
    )

    // INC-0127: asignada a Luis Ramírez y lista para que empiece a atenderla (HU-08)
    private val asignada = Incidencia(
        id = "i127", numero = 127, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "María Quispe", fechaRegistro = t0.plusSeconds(120 * 60), registradoPorId = "demo-pcardenas",
        registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.ASIGNADA,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private fun TestScope.nuevoVm(
        id: String = "c",
        inc: Incidencia = incidencia
    ): DetalleIncidenciaViewModel {
        val repo = IncidenciaRepositoryFalso().also { it.cargar(listOf(inc), movimientos) }
        val vm = DetalleIncidenciaViewModel(
            SavedStateHandle(mapOf(DetalleIncidenciaViewModel.ARG_INCIDENCIA_ID to id)),
            ObservarDetalleIncidencia(repo),
            ObservarUsuarioActual(sesion),
            IniciarAtencion(VerificarPermiso(sesion), repo)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    // HU-08: variante de nuevoVm() que también entrega el repo, para inspeccionar sus movimientos
    // después de alIniciarAtencion(); nuevoVm() no lo expone porque los casos de HU-04/06/07 no lo necesitan.
    private fun TestScope.nuevoVmConRepo(inc: Incidencia): Pair<DetalleIncidenciaViewModel, IncidenciaRepositoryFalso> {
        val repo = IncidenciaRepositoryFalso().also { it.cargar(listOf(inc)) }
        val vm = DetalleIncidenciaViewModel(
            SavedStateHandle(mapOf(DetalleIncidenciaViewModel.ARG_INCIDENCIA_ID to inc.id)),
            ObservarDetalleIncidencia(repo),
            ObservarUsuarioActual(sesion),
            IniciarAtencion(VerificarPermiso(sesion), repo)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm to repo
    }

    @Test
    fun `la linea de tiempo muestra el movimiento mas reciente arriba`() = runTest {
        val vm = nuevoVm()
        val tipos = vm.estado.value.detalle!!.movimientos.map { it.tipo }
        assertEquals(
            listOf(TipoMovimiento.ATENCION_INICIADA, TipoMovimiento.ASIGNADA, TipoMovimiento.REGISTRADA),
            tipos
        )
    }

    @Test
    fun `el jefe asigna y el tecnico asignado atiende`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val jefe = nuevoVm().estado.value
        assertTrue(jefe.puedeAsignar)
        assertFalse(jefe.puedeAtender)

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val tecnicoAsignado = nuevoVm().estado.value
        assertFalse(tecnicoAsignado.puedeAsignar)
        assertTrue(tecnicoAsignado.puedeAtender)

        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        val sistemas = nuevoVm().estado.value
        assertFalse(sistemas.puedeAsignar)
        assertFalse(sistemas.puedeAtender)
    }

    @Test
    fun `otro tecnico no puede atender una incidencia que no es suya`() = runTest {
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        val ana = nuevoVm().estado.value
        assertFalse(ana.puedeAtender)
        assertFalse(ana.puedeAsignar)
    }

    @Test
    fun `una incidencia solucionada no se asigna ni se atiende`() = runTest {
        val solucionada = incidencia.copy(marca = MarcaSeguimiento.CERRADA)

        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        assertFalse(nuevoVm(inc = solucionada).estado.value.puedeAsignar)

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertFalse(nuevoVm(inc = solucionada).estado.value.puedeAtender)
    }

    @Test
    fun `un id que no existe se informa como no encontrada`() = runTest {
        val estado = nuevoVm(id = "no-existe").estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrada)
    }

    @Test
    fun `ahora avanza cada minuto sin que cambien el detalle ni los permisos del usuario`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVm()
        val primera = vm.estado.value.ahora
        val detalleAntes = vm.estado.value.detalle            // incluye los movimientos (línea de tiempo)
        val puedeAsignarAntes = vm.estado.value.puedeAsignar
        val puedeAtenderAntes = vm.estado.value.puedeAtender   // depende del usuario observado

        // El reloj del ViewModel usa Instant.now() real (no es inyectable, ver plan.md sección 1):
        // una espera real breve asegura que el reloj del sistema avance entre ambas lecturas,
        // porque su resolución puede ser más gruesa que el tiempo que tarda en ejecutarse esta prueba.
        Thread.sleep(50)

        // El delay() del reloj corre en viewModelScope (Dispatchers.Main), cuyo scheduler de prueba
        // es el de MainDispatcherRule, no el de este runTest; hay que avanzar ese scheduler, no el propio.
        // RELOJ_MS es privado en el ViewModel; 60_000L replica su valor (un minuto).
        // advanceTimeBy no ejecuta lo programado justo en el nuevo instante; runCurrent lo hace.
        main.dispatcher.scheduler.advanceTimeBy(60_000L)
        main.dispatcher.scheduler.runCurrent()

        assertNotEquals(primera, vm.estado.value.ahora)   // el temporizador funciona sin datos nuevos
        assertEquals(detalleAntes, vm.estado.value.detalle)
        assertEquals(puedeAsignarAntes, vm.estado.value.puedeAsignar)
        assertEquals(puedeAtenderAntes, vm.estado.value.puedeAtender)
    }

    @Test
    fun `puedeIniciarAtencion es verdadero en marca Asignada para el tecnico asignado`() = runTest {
        // RF-806
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVmConRepo(asignada).first

        assertTrue(vm.estado.value.puedeIniciarAtencion)
    }

    @Test
    fun `puedeIniciarAtencion es falso si la marca no es Asignada`() = runTest {
        // RF-806: ya cambió de marca (En atención, Esperando repuesto, Cerrada, Sin asignar)
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = nuevoVmConRepo(asignada.copy(marca = MarcaSeguimiento.EN_ATENCION)).first

        assertFalse(vm.estado.value.puedeIniciarAtencion)
    }

    @Test
    fun `puedeIniciarAtencion es falso para un tecnico distinto al asignado`() = runTest {
        // RF-806, RF-807: aunque esté en marca Asignada, no es su incidencia
        sesion.iniciarSesion("atorres@softcorp.pe", "")
        val vm = nuevoVmConRepo(asignada).first

        assertFalse(vm.estado.value.puedeIniciarAtencion)
    }

    @Test
    fun `alIniciarAtencion deshabilita el boton mientras guarda y no duplica el movimiento`() = runTest {
        // RF-811: un segundo toque inmediato no debe crear un segundo movimiento
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val (vm, repo) = nuevoVmConRepo(asignada)

        vm.alIniciarAtencion()
        vm.alIniciarAtencion()

        assertTrue(vm.estado.value.iniciandoAtencion)
        assertEquals(1, repo.movimientos.size)
        assertEquals(MarcaSeguimiento.EN_ATENCION, repo.incidencias.single().marca)
    }

    @Test
    fun `el error de IniciarAtencion queda en errorAtencion`() = runTest {
        // RF-808: la marca ya cambió por otra vía antes de que el toque se guardara
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val (vm, repo) = nuevoVmConRepo(asignada)
        // Cambia la marca justo antes del toque, simulando la carrera que describe RF-808
        repo.cargar(listOf(asignada.copy(marca = MarcaSeguimiento.EN_ATENCION)))

        vm.alIniciarAtencion()

        assertEquals(ErrorDominio.MarcaCambiada, vm.estado.value.errorAtencion)
        assertFalse(vm.estado.value.iniciandoAtencion)
        assertTrue(repo.movimientos.isEmpty())
    }
}