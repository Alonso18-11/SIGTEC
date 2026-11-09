package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.diccionario.data.DiccionarioRepositoryFalso
import com.softcorp.sigtec.feature.diccionario.domain.RegistrarFalla
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import com.softcorp.sigtec.feature.incidencias.domain.ObservarDetalleIncidencia
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-13 (RF-1310 a RF-1318): validación, dictado, guardado y formulario lleno desde una incidencia cerrada. */
class RegistrarFallaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val diccionario = DiccionarioRepositoryFalso()
    private val incidencias = IncidenciaRepositoryFalso()
    private val fecha = Instant.parse("2026-11-01T10:00:00Z")

    private fun crear(origen: String? = null) = RegistrarFallaViewModel(
        SavedStateHandle(mapOf(RegistrarFallaViewModel.ARG_INCIDENCIA_ORIGEN_ID to origen)),
        RegistrarFalla(VerificarPermiso(sesion), diccionario),
        ObservarDetalleIncidencia(incidencias)
    )

    private fun RegistrarFallaViewModel.llenar() {
        alCambiarTitulo("Pantalla azul al iniciar")
        alCambiarTipo(TipoFalla.SOFTWARE)
        alCambiarComponente("Sistema operativo")
        alCambiarSintomas("Se reinicia")
        alCambiarSolucion("Modo seguro")
    }

    @Test
    fun `no deja guardar hasta llenar los cinco campos sin espacios`() {
        val vm = crear()
        assertFalse(vm.estado.value.puedeGuardar)
        vm.llenar()
        assertTrue(vm.estado.value.puedeGuardar)
        vm.alCambiarComponente("   ")
        assertFalse(vm.estado.value.puedeGuardar)
    }

    @Test
    fun `el dictado se agrega al final de sintomas y solucion`() {
        val vm = crear()
        vm.alCambiarSintomas("Se reinicia")
        vm.alRecibirDictado(CampoDictado.SINTOMAS, "con pantalla azul")
        vm.alRecibirDictado(CampoDictado.SOLUCION, "Revertir el controlador")
        vm.alRecibirDictado(CampoDictado.DESCRIPCION, "no aplica")
        assertEquals("Se reinicia con pantalla azul", vm.estado.value.sintomas)
        assertEquals("Revertir el controlador", vm.estado.value.solucion)
    }

    @Test
    fun `el tecnico guarda y se avisa una sola vez`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        val vm = crear()
        vm.llenar()

        vm.alGuardar()

        assertTrue(vm.estado.value.guardada)
        assertEquals("Pantalla azul al iniciar", diccionario.fallas.single().titulo)
        assertNull(diccionario.fallas.single().incidenciaOrigenId)
        vm.alSalir()
        assertFalse(vm.estado.value.guardada)
    }

    @Test
    fun `sin permiso muestra el error y no guarda`() = runTest {
        sesion.iniciarSesion("pcardenas@softcorp.pe", "")
        val vm = crear()
        vm.llenar()

        vm.alGuardar()

        assertEquals(ErrorDominio.SinPermiso, vm.estado.value.error)
        assertFalse(vm.estado.value.guardando)
        assertTrue(diccionario.fallas.isEmpty())
    }

    @Test
    fun `desde una incidencia cerrada el formulario se llena con su informe y guarda el origen`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        incidencias.cargar(
            incidencias = listOf(
                Incidencia(
                    id = "inc-1", numero = 13, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no imprime",
                    usuarioResponsable = "María Quispe", fechaRegistro = fecha, registradoPorId = "demo-lramirez",
                    registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.CERRADA, tipoFalla = TipoFalla.RED
                )
            ),
            informes = listOf(
                InformeTecnico(
                    id = "inf-1", incidenciaId = "inc-1", diagnostico = "IP duplicada en la red",
                    trabajoRealizado = "Asignar una IP fija", piezasCambiadas = null, observaciones = null,
                    fechaCierre = fecha, confirmadoEn = fecha
                )
            )
        )

        val vm = crear(origen = "inc-1")

        val e = vm.estado.value
        assertEquals("La impresora no imprime", e.titulo)
        assertEquals(TipoFalla.RED, e.tipo)
        assertEquals("IP duplicada en la red", e.sintomas)
        assertEquals("Asignar una IP fija", e.solucion)
        assertEquals("", e.componente)
        assertEquals("INC-0013 - IMP-RRHH-002", e.origen)

        vm.alCambiarComponente("Impresora")
        vm.alGuardar()

        assertEquals("inc-1", diccionario.fallas.single().incidenciaOrigenId)
    }
}
