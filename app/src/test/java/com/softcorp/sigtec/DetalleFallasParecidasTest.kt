package com.softcorp.sigtec

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryFalso
import com.softcorp.sigtec.feature.captura.data.LinternaFalsa
import com.softcorp.sigtec.feature.captura.domain.EliminarEvidencia
import com.softcorp.sigtec.feature.diccionario.data.DiccionarioRepositoryFalso
import com.softcorp.sigtec.feature.diccionario.domain.EstaEnDiccionario
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba
import com.softcorp.sigtec.feature.diccionario.domain.ObservarDiccionario
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-13: fallas parecidas (RF-1324 a RF-1330) y «Guardar en el diccionario» (RF-1316, RF-1319) en el detalle. */
class DetalleFallasParecidasTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val diccionario = DiccionarioRepositoryFalso(FallasDePrueba.todas)
    private val t0 = Instant.parse("2026-11-09T10:00:00Z")

    private val pendiente = Incidencia(
        id = "i1", numero = 20, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no imprime desde la red",
        usuarioResponsable = "María Quispe", fechaRegistro = t0, registradoPorId = "demo-pcardenas",
        registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.ASIGNADA,
        tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )
    private val cerrada = pendiente.copy(marca = MarcaSeguimiento.CERRADA)
    private val informe = InformeTecnico(
        id = "inf", incidenciaId = "i1", diagnostico = "IP duplicada", trabajoRealizado = "IP fija",
        piezasCambiadas = null, observaciones = null, fechaCierre = t0, confirmadoEn = t0
    )

    private suspend fun TestScope.vm(correo: String, inc: Incidencia, conInforme: Boolean = false): DetalleIncidenciaViewModel {
        sesion.iniciarSesion(correo, "")
        val repo = IncidenciaRepositoryFalso().also {
            it.cargar(listOf(inc), informes = if (conInforme) listOf(informe) else emptyList())
        }
        val vm = DetalleIncidenciaViewModel(
            SavedStateHandle(mapOf(DetalleIncidenciaViewModel.ARG_INCIDENCIA_ID to inc.id)),
            ObservarDetalleIncidencia(repo),
            ObservarUsuarioActual(sesion),
            IniciarAtencion(VerificarPermiso(sesion), repo),
            EliminarEvidencia(VerificarPermiso(sesion), EvidenciaRepositoryFalso()),
            LinternaFalsa(),
            ObservarDiccionario(VerificarPermiso(sesion), diccionario),
            EstaEnDiccionario(diccionario),
            main.dispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun `el tecnico ve las fallas parecidas de una incidencia pendiente`() = runTest {
        val vm = vm("lramirez@softcorp.pe", pendiente)
        assertEquals(FallasDePrueba.impresora, vm.estado.value.fallasParecidas?.first())
    }

    @Test
    fun `el personal de sistemas tambien las ve y el jefe no`() = runTest {
        assertTrue(vm("pcardenas@softcorp.pe", pendiente).estado.value.fallasParecidas!!.isNotEmpty())
        assertNull(vm("cmendoza@softcorp.pe", pendiente).estado.value.fallasParecidas)
    }

    @Test
    fun `sin nada parecido la seccion queda vacia y en una solucionada no aparece`() = runTest {
        val sinParecido = pendiente.copy(descripcion = "El mouse hace doble clic")
        assertEquals(emptyList<Any>(), vm("lramirez@softcorp.pe", sinParecido).estado.value.fallasParecidas)
        assertNull(vm("lramirez@softcorp.pe", cerrada).estado.value.fallasParecidas)
    }

    @Test
    fun `una falla nueva del diccionario cambia las sugerencias`() = runTest {
        val vm = vm("lramirez@softcorp.pe", pendiente.copy(descripcion = "El mouse hace doble clic"))
        val mouse = FallasDePrueba.falla("f9", "Mouse con doble clic", "Mouse", "Hace doble clic solo")

        diccionario.agregar(mouse)

        assertEquals(listOf(mouse), vm.estado.value.fallasParecidas)
    }

    @Test
    fun `guardar en el diccionario solo con informe y para el tecnico, y luego ya esta`() = runTest {
        val tecnico = vm("lramirez@softcorp.pe", cerrada, conInforme = true)
        assertTrue(tecnico.estado.value.puedeGuardarEnDiccionario)
        assertFalse(tecnico.estado.value.yaEnDiccionario)

        diccionario.agregar(FallasDePrueba.falla("f8", "IP duplicada", "Impresora", "No imprime", origen = "i1"))
        assertTrue(tecnico.estado.value.yaEnDiccionario)

        assertFalse(vm("lramirez@softcorp.pe", cerrada, conInforme = false).estado.value.puedeGuardarEnDiccionario)
        assertFalse(vm("pcardenas@softcorp.pe", cerrada, conInforme = true).estado.value.puedeGuardarEnDiccionario)
    }
}
