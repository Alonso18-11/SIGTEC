package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import com.softcorp.sigtec.feature.atencion.data.LogisticaRepositoryFalso
import com.softcorp.sigtec.feature.atencion.domain.Disponibilidad
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import com.softcorp.sigtec.feature.atencion.domain.SolicitarRepuesto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** HU-11 (RF-1104 a RF-1116): buscar la pieza mientras se escribe y enviar con la confirmación. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SolicitudRepuestoViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sesion = SesionRepositoryDemo()
    private val atencion = AtencionRepositoryFalso()
    private val logistica = LogisticaRepositoryFalso()
    private val confirmadoEn = Instant.parse("2026-11-07T15:30:00Z")

    private val enAtencion = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-03T09:15:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private suspend fun nuevoVm(
        disponibilidad: DisponibilidadBiometrica = DisponibilidadBiometrica.HUELLA
    ): SolicitudRepuestoViewModel {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        atencion.cargar(listOf(enAtencion))
        return SolicitudRepuestoViewModel(
            SavedStateHandle(mapOf(SolicitudRepuestoViewModel.ARG_INCIDENCIA_ID to "i124")),
            SolicitarRepuesto(VerificarPermiso(sesion), atencion),
            atencion,
            logistica,
            disponibilidad
        )
    }

    private val SolicitudRepuestoViewModel.ui get() = estado.value

    private fun esperar(ms: Long) {
        main.dispatcher.scheduler.advanceTimeBy(ms)
        main.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `muestra la incidencia para la que se pide`() = runTest {
        assertEquals("i124", nuevoVm().ui.incidencia?.id)   // RF-1101
    }

    @Test
    fun `con menos de 3 letras no consulta a Logistica`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fu")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)

        assertTrue(logistica.busquedas.isEmpty())   // RF-1104
    }

    @Test
    fun `consulta cuando deja de escribir 0,6 segundos`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fuente")

        esperar(SolicitudRepuestoViewModel.ESPERA_MS - 1)
        assertTrue(logistica.busquedas.isEmpty())

        esperar(1)
        assertEquals(listOf("fuente"), logistica.busquedas)   // RF-1104
        assertEquals("Fuente de poder ATX 500 W", vm.ui.coincidencias.single().nombre)
    }

    @Test
    fun `si sigue escribiendo solo cuenta la ultima busqueda`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fue")
        esperar(300)
        vm.alCambiarPieza("disco")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)

        assertEquals(listOf("disco"), logistica.busquedas)   // RF-1106
        assertEquals("Disco SSD 480 GB", vm.ui.coincidencias.single().nombre)
    }

    @Test
    fun `elegir una pieza con stock muestra la disponibilidad`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fuente")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)

        vm.alElegirPieza(vm.ui.coincidencias.single())

        assertEquals("Fuente de poder ATX 500 W", vm.ui.pieza)
        assertEquals(Disponibilidad.ConStock(3, 1), vm.ui.disponibilidad)   // RF-1105
        assertTrue(vm.ui.coincidencias.isEmpty())
    }

    @Test
    fun `elegir una pieza sin stock avisa y deja pedir igual`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("mouse")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)
        vm.alElegirPieza(vm.ui.coincidencias.single())
        vm.alCambiarMotivo("El mouse no responde")

        assertEquals(Disponibilidad.SinStock(3), vm.ui.disponibilidad)   // D-3
        assertTrue(vm.ui.puedeEnviar)
    }

    @Test
    fun `sin coincidencias avisa que no esta en el catalogo`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("placa madre")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)

        assertEquals(Disponibilidad.NoEnCatalogo, vm.ui.disponibilidad)   // RF-1107
    }

    @Test
    fun `si Logistica no responde avisa y reintentar vuelve a consultar`() = runTest {
        logistica.fallaBusqueda = ErrorLogistica.SIN_RESPUESTA
        val vm = nuevoVm()
        vm.alCambiarPieza("fuente")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)
        assertEquals(Disponibilidad.SinRespuesta, vm.ui.disponibilidad)   // RF-1108

        logistica.fallaBusqueda = null
        vm.alReintentar()

        assertEquals(2, logistica.busquedas.size)
        assertEquals(1, vm.ui.coincidencias.size)
    }

    @Test
    fun `editar la pieza elegida quita su disponibilidad`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fuente")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)
        vm.alElegirPieza(vm.ui.coincidencias.single())

        vm.alCambiarPieza("Fuente de poder ATX 600 W")

        assertNull(vm.ui.elegida)   // RF-1109
        assertNull(vm.ui.disponibilidad)
    }

    @Test
    fun `sin pieza, sin motivo o sin seguridad en el celular no se puede enviar`() = runTest {
        val vm = nuevoVm()
        assertFalse(vm.ui.puedeEnviar)   // RF-1110
        vm.alCambiarPieza("Fuente de poder ATX 500 W")
        assertFalse(vm.ui.puedeEnviar)
        vm.alCambiarMotivo("   ")
        assertFalse(vm.ui.puedeEnviar)
        vm.alCambiarMotivo("No entrega energía")
        assertTrue(vm.ui.puedeEnviar)

        val sinSeguridad = nuevoVm(DisponibilidadBiometrica.SIN_SEGURIDAD)
        sinSeguridad.alCambiarPieza("Fuente de poder ATX 500 W")
        sinSeguridad.alCambiarMotivo("No entrega energía")
        assertFalse(sinSeguridad.ui.puedeEnviar)
    }

    @Test
    fun `enviar guarda con la disponibilidad elegida y vuelve al detalle`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("fuente")
        esperar(SolicitudRepuestoViewModel.ESPERA_MS)
        vm.alElegirPieza(vm.ui.coincidencias.single())
        vm.alCambiarMotivo("No entrega energía")

        vm.alConfirmar(confirmadoEn)

        val solicitud = atencion.solicitudes.single()
        assertEquals(confirmadoEn, solicitud.confirmadoEn)   // RF-1113
        assertEquals(3, solicitud.stockConsultado)
        assertEquals(solicitud.id, vm.ui.registradaId)       // RF-1114
        vm.alVolverAlDetalle()
        assertNull(vm.ui.registradaId)
    }

    @Test
    fun `sin consultar se envia igual sin disponibilidad`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("Placa madre H610")
        vm.alCambiarMotivo("No da video")

        vm.alConfirmar(confirmadoEn)

        assertNull(atencion.solicitudes.single().stockConsultado)   // D-2
    }

    @Test
    fun `un doble toque registra una sola solicitud`() = runTest {
        val vm = nuevoVm()
        vm.alCambiarPieza("Fuente de poder ATX 500 W")
        vm.alCambiarMotivo("No entrega energía")

        vm.alConfirmar(confirmadoEn)
        vm.alConfirmar(confirmadoEn)

        assertEquals(1, atencion.solicitudes.size)   // RF-1116
        assertTrue(vm.ui.enviando)
    }

    @Test
    fun `si Room falla muestra el error y conserva lo escrito`() = runTest {
        val vm = nuevoVm()
        atencion.fallar = true
        vm.alCambiarPieza("Fuente de poder ATX 500 W")
        vm.alCambiarMotivo("No entrega energía")

        vm.alConfirmar(confirmadoEn)

        assertNotNull(vm.ui.error as? ErrorDominio.Desconocido)   // RF-1115
        assertFalse(vm.ui.enviando)
        assertEquals("No entrega energía", vm.ui.motivo)
        assertNull(vm.ui.registradaId)
    }
}
