package com.softcorp.sigtec

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.incidencias.data.DirectorioRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import com.softcorp.sigtec.feature.incidencias.presentation.EstadoDirectorio
import com.softcorp.sigtec.feature.incidencias.domain.RegistrarIncidencia
import com.softcorp.sigtec.feature.incidencias.presentation.RegistroIncidenciaUiState
import com.softcorp.sigtec.feature.incidencias.presentation.RegistroIncidenciaViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// HU-03 (specs/003-registro-incidencia)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RegistroIncidenciaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val repo = IncidenciaRepositoryFalso()
    private val directorio = DirectorioRepositoryFalso()
    private val maria = Colaborador("María Quispe Huamán", "Contabilidad", "2314")

    private fun TestScope.nuevoVm(): RegistroIncidenciaViewModel {
        val vm = RegistroIncidenciaViewModel(
            RegistrarIncidencia(VerificarPermiso(sesion), repo),
            ObservarUsuarioActual(sesion),
            repo,
            directorio
        )
        // WhileSubscribed necesita a alguien mirando; Unconfined lo pone a mirar de inmediato
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private val RegistroIncidenciaViewModel.ui: RegistroIncidenciaUiState get() = estado.value

    @Test
    fun `sin sesion o con un campo vacio el boton queda deshabilitado`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.ui.puedeGuardar)   // sin sesión y sin datos

        vm.alCambiarCodigo("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")
        assertFalse(vm.ui.puedeGuardar)   // formulario completo, pero todavía sin sesión

        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertTrue(vm.ui.puedeGuardar)   // con sesión y los tres campos completos, ya se puede guardar

        vm.alCambiarResponsable("")
        assertFalse(vm.ui.puedeGuardar)   // un campo vuelve a quedar vacío
    }

    @Test
    fun `la fecha mostrada avanza cada minuto mientras la pantalla esta abierta`() = runTest {
        val vm = nuevoVm()
        val primera = vm.ui.fechaHora

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

        val segunda = vm.ui.fechaHora
        assertNotEquals(primera, segunda)   // no se fija el valor exacto, solo que cambió
    }

    @Test
    fun `guardar con exito deja registradaId y alNavegarAlDetalle lo limpia`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")

        vm.guardar()
        // advanceUntilIdle() no sirve aquí: el reloj de fechaHora reprograma un delay() infinito
        // en el mismo scheduler de Main y nunca queda "idle"; runCurrent() basta porque guardar()
        // no depende de ningún delay().
        main.dispatcher.scheduler.runCurrent()

        val id = vm.ui.registradaId
        assertNotNull(id)                               // RF-40: evento de un solo uso
        assertEquals(repo.incidencias.single().id, id)   // coincide con el id real guardado

        vm.alNavegarAlDetalle()
        assertNull(vm.ui.registradaId)
    }

    @Test
    fun `guardar con un equipo fuera del catalogo deja el error y permite reintentar`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-14")   // fuera del catálogo falso, a diferencia de "PC-CONT-014"
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")

        vm.guardar()
        // advanceUntilIdle() no sirve aquí: el reloj de fechaHora reprograma un delay() infinito
        // en el mismo scheduler de Main y nunca queda "idle"; runCurrent() basta porque guardar()
        // no depende de ningún delay().
        main.dispatcher.scheduler.runCurrent()

        assertEquals(ErrorDominio.EquipoNoEncontrado, vm.ui.error)   // RF-41
        assertNull(vm.ui.registradaId)
        assertFalse(vm.ui.guardando)   // se puede reintentar
    }

    // HU-09 (specs/009-escaneo-evidencias)

    @Test
    fun `el codigo escaneado reemplaza el escrito y muestra que vino de la camara`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCodigo("PC-LOG-9")
        assertFalse(vm.ui.codigoLeidoConCamara)

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertEquals("PC-CONT-014", vm.ui.codigoEquipo)   // RF-910
        assertTrue(vm.ui.codigoLeidoConCamara)
    }

    @Test
    fun `editar a mano el codigo escaneado quita la indicacion de la camara`() = runTest {
        val vm = nuevoVm()
        vm.alUsarCodigoEscaneado("PC-CONT-014")

        vm.alCambiarCodigo("PC-CONT-01")

        assertEquals("PC-CONT-01", vm.ui.codigoEquipo)
        assertFalse(vm.ui.codigoLeidoConCamara)   // RF-911
    }

    @Test
    fun `recibir un codigo escaneado limpia el error anterior`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-14")
        vm.alCambiarDescripcion("No enciende tras el corte de luz")
        vm.alCambiarResponsable("María Quispe")
        vm.guardar()
        main.dispatcher.scheduler.runCurrent()
        assertEquals(ErrorDominio.EquipoNoEncontrado, vm.ui.error)

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertNull(vm.ui.error)
    }

    // HU-10 (specs/010-dictado-linterna)

    @Test
    fun `lo dictado en una descripcion vacia queda tal cual`() = runTest {
        val vm = nuevoVm()

        vm.alRecibirDictado("No enciende tras el corte de luz")

        assertEquals("No enciende tras el corte de luz", vm.ui.descripcion)   // RF-1011
    }

    @Test
    fun `lo dictado se agrega al final de lo escrito`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarDescripcion("No enciende.")

        vm.alRecibirDictado("Huele a quemado")

        assertEquals("No enciende. Huele a quemado", vm.ui.descripcion)   // RF-1011, D-3
    }

    @Test
    fun `durante el guardado lo dictado no cambia la descripcion`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alCambiarCodigo("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende")
        vm.alCambiarResponsable("María Quispe")
        vm.guardar()
        main.dispatcher.scheduler.runCurrent()
        assertTrue(vm.ui.guardando)

        vm.alRecibirDictado("Huele a quemado")

        assertEquals("No enciende", vm.ui.descripcion)   // RF-1002
    }

    // HU-12 (specs/012-directorio-colaboradores)

    /** La espera de 0,6 s corre en viewModelScope, con el scheduler de MainDispatcherRule. */
    private fun avanzar(ms: Long) {
        main.dispatcher.scheduler.advanceTimeBy(ms)
        main.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `al dejar de escribir 0,6 s completa nombre, area y anexo`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCodigo("PC-CONT-014")

        avanzar(500)
        assertTrue(directorio.consultas.isEmpty())   // RF-1201: todavía escribiendo

        avanzar(100)
        assertEquals(listOf("PC-CONT-014"), directorio.consultas)
        assertEquals("María Quispe Huamán", vm.ui.responsable)   // RF-1206
        assertTrue(vm.ui.responsableDelDirectorio)
        assertEquals("Contabilidad, anexo 2314", vm.ui.areaYAnexo)
    }

    @Test
    fun `un codigo incompleto o fuera del catalogo no consulta`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCodigo("PC-CONT-01")
        avanzar(600)

        assertTrue(directorio.consultas.isEmpty())   // RF-1201, D-5
        assertEquals(EstadoDirectorio.Inactivo, vm.ui.directorio)
    }

    @Test
    fun `el codigo escaneado consulta sin esperar`() = runTest {
        val vm = nuevoVm()

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertEquals(listOf("PC-CONT-014"), directorio.consultas)   // RF-1202
        assertEquals("María Quispe Huamán", vm.ui.responsable)
    }

    @Test
    fun `mientras consulta muestra buscando y solo cuenta el ultimo codigo`() = runTest {
        val vm = nuevoVm()
        val espera = CompletableDeferred<Unit>()
        directorio.enEspera = espera

        vm.alUsarCodigoEscaneado("PC-LOG-009")
        assertEquals(EstadoDirectorio.Buscando, vm.ui.directorio)   // RF-1203

        vm.alUsarCodigoEscaneado("PC-CONT-014")
        espera.complete(Unit)

        assertEquals(listOf("PC-LOG-009", "PC-CONT-014"), directorio.consultas)
        assertEquals("María Quispe Huamán", vm.ui.responsable)   // RF-1204: la de PC-LOG-009 se canceló
    }

    @Test
    fun `escribir rapido solo consulta el codigo final`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarCodigo("PC-LOG-009")
        avanzar(300)
        vm.alCambiarCodigo("PC-CONT-014")
        avanzar(600)

        assertEquals(listOf("PC-CONT-014"), directorio.consultas)   // RF-1204
    }

    @Test
    fun `no pisa lo escrito a mano y Usar lo reemplaza`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarResponsable("Juan Pérez")

        vm.alUsarCodigoEscaneado("PC-CONT-014")

        assertEquals("Juan Pérez", vm.ui.responsable)   // RF-1207, D-6
        assertEquals(maria, vm.ui.sugerenciaDirectorio)
        assertNull(vm.ui.areaYAnexo)

        vm.alUsarDelDirectorio()

        assertEquals("María Quispe Huamán", vm.ui.responsable)
        assertTrue(vm.ui.responsableDelDirectorio)
        assertNull(vm.ui.sugerenciaDirectorio)
    }

    @Test
    fun `editar el nombre completado quita el area y deja Usar`() = runTest {
        val vm = nuevoVm()
        vm.alUsarCodigoEscaneado("PC-CONT-014")

        vm.alCambiarResponsable("María Quispe")

        assertFalse(vm.ui.responsableDelDirectorio)   // RF-1208, D-4
        assertNull(vm.ui.areaYAnexo)
        assertNull(vm.ui.areaParaGuardar)
        assertEquals(maria, vm.ui.sugerenciaDirectorio)
    }

    @Test
    fun `cambiar el codigo vacia lo completado pero no lo escrito a mano`() = runTest {
        val vm = nuevoVm()
        vm.alUsarCodigoEscaneado("PC-CONT-014")

        vm.alCambiarCodigo("PC-CONT-01")

        assertEquals("", vm.ui.responsable)   // RF-1209
        assertFalse(vm.ui.responsableDelDirectorio)
        assertEquals(EstadoDirectorio.Inactivo, vm.ui.directorio)

        vm.alCambiarResponsable("Juan Pérez")
        vm.alCambiarCodigo("PC-CONT-0")
        assertEquals("Juan Pérez", vm.ui.responsable)
    }

    @Test
    fun `sin responsable en el directorio deja el campo libre`() = runTest {
        val vm = nuevoVm()

        vm.alUsarCodigoEscaneado("PC-SIS-003")   // está en el catálogo, no en el directorio falso

        assertEquals(EstadoDirectorio.NoEncontrado, vm.ui.directorio)   // RF-1211
        assertEquals("", vm.ui.responsable)
    }

    @Test
    fun `sin respuesta avisa y Reintentar vuelve a consultar`() = runTest {
        val vm = nuevoVm()
        directorio.falla = ErrorDirectorio.SIN_RESPUESTA

        vm.alUsarCodigoEscaneado("PC-CONT-014")
        assertEquals(EstadoDirectorio.SinRespuesta, vm.ui.directorio)   // RF-1212
        assertEquals("", vm.ui.responsable)

        directorio.falla = null
        vm.alReintentarDirectorio()

        assertEquals(listOf("PC-CONT-014", "PC-CONT-014"), directorio.consultas)
        assertEquals("María Quispe Huamán", vm.ui.responsable)
    }

    @Test
    fun `guardar lleva el area solo si el nombre vino del directorio`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        vm.alUsarCodigoEscaneado("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende")

        vm.guardar()
        main.dispatcher.scheduler.runCurrent()

        assertEquals("Contabilidad", repo.incidencias.single().areaResponsable)   // RF-1214
    }

    @Test
    fun `sin directorio se puede guardar escribiendo a mano`() = runTest {
        val vm = nuevoVm()
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        directorio.falla = ErrorDirectorio.SIN_RESPUESTA
        vm.alUsarCodigoEscaneado("PC-CONT-014")
        vm.alCambiarDescripcion("No enciende")
        vm.alCambiarResponsable("María Quispe")

        assertTrue(vm.ui.puedeGuardar)   // RF-1213
        vm.guardar()
        main.dispatcher.scheduler.runCurrent()

        assertNull(repo.incidencias.single().areaResponsable)
    }
}
