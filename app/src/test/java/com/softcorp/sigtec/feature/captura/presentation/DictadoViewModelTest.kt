package com.softcorp.sigtec.feature.captura.presentation

import androidx.lifecycle.SavedStateHandle
import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.feature.captura.data.ReconocedorVozFalso
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado
import com.softcorp.sigtec.feature.captura.domain.EventoDictado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** HU-10 (RF-1004 a RF-1018, D-10): escuchar hasta «Detener», el tope de silencio, los errores y entregar el texto. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DictadoViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val reconocedor = ReconocedorVozFalso()

    private fun nuevoVm(campo: CampoDictado = CampoDictado.DESCRIPCION) = DictadoViewModel(
        SavedStateHandle(
            mapOf(
                DictadoViewModel.ARG_CAMPO to campo.name,
                DictadoViewModel.ARG_SUBTITULO to "Nueva incidencia · PC-CONT-014"
            )
        ),
        reconocedor
    )

    private val DictadoViewModel.ui get() = estado.value

    /** Deja pasar el tiempo virtual: así corre la pausa antes de volver a escuchar y el tope de silencio. */
    private fun avanzar(ms: Long) {
        main.dispatcher.scheduler.advanceTimeBy(ms)
        main.dispatcher.scheduler.runCurrent()
    }

    /** Lo que hace Android al pulsar «Detener»: entrega lo reconocido y termina la escucha. */
    private fun DictadoViewModel.detenerCon(final: String) {
        alDetener()
        reconocedor.emitir(EventoDictado.Final(final))
    }

    @Test
    fun `lee el campo y el subtitulo de la ruta`() {
        val vm = nuevoVm(CampoDictado.DIAGNOSTICO)
        assertEquals(CampoDictado.DIAGNOSTICO, vm.ui.campo)
        assertEquals("Nueva incidencia · PC-CONT-014", vm.ui.subtitulo)
    }

    @Test
    fun `con el permiso concedido empieza a escuchar solo una vez`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando)
        assertTrue(vm.ui.escuchando)

        vm.detenerCon("No enciende")
        vm.alPermisoConcedido()   // RF-1014: al volver a la pantalla no vuelve a escuchar solo

        assertEquals(1, reconocedor.escuchas)
        assertFalse(vm.ui.escuchando)
    }

    @Test
    fun `los resultados parciales se ven mientras habla`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No"), EventoDictado.Parcial("No enciende"))

        assertEquals("No enciende", vm.ui.textoVisible)
        assertTrue(vm.ui.puedeUsar)
    }

    @Test
    fun `sin texto no se puede usar`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando)
        assertFalse(vm.ui.puedeUsar)   // RF-1009
    }

    @Test
    fun `una pausa no detiene la escucha suma el tramo y vuelve a escuchar`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende."))
        reconocedor.emitir(EventoDictado.Final("No enciende."))   // Android cortó por la pausa

        assertTrue(vm.ui.escuchando)   // D-10: sigue escuchando
        assertEquals("No enciende.", vm.ui.texto)

        avanzar(DictadoViewModel.REINICIO_MS)
        reconocedor.emitir(EventoDictado.Parcial("Huele a quemado"))

        assertEquals(2, reconocedor.escuchas)
        assertEquals("No enciende. Huele a quemado", vm.ui.textoVisible)   // RF-1008
    }

    @Test
    fun `un silencio mientras escucha no muestra error y vuelve a escuchar`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Error(ErrorDictado.SIN_ENTENDER))

        assertNull(vm.ui.error)
        assertTrue(vm.ui.escuchando)
        avanzar(DictadoViewModel.REINICIO_MS)
        assertEquals(2, reconocedor.escuchas)
    }

    @Test
    fun `detener entrega lo reconocido y ya no vuelve a escuchar`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende"))

        vm.detenerCon("No enciende")
        avanzar(DictadoViewModel.REINICIO_MS)

        assertEquals(1, reconocedor.detenciones)
        assertEquals(1, reconocedor.escuchas)
        assertEquals("No enciende", vm.ui.texto)
        assertFalse(vm.ui.escuchando)   // RF-1007
    }

    @Test
    fun `si al detener el final llega vacio se conserva lo que se veia`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende"))

        vm.detenerCon("")

        assertEquals("No enciende", vm.ui.texto)   // RF-1007
        assertTrue(vm.ui.puedeUsar)
    }

    @Test
    fun `detener durante la pausa entre tramos no vuelve a escuchar`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende"))

        vm.alDetener()
        avanzar(DictadoViewModel.REINICIO_MS)

        assertEquals(1, reconocedor.escuchas)
        assertFalse(vm.ui.escuchando)
    }

    @Test
    fun `se detiene solo tras el tope sin palabras nuevas`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Parcial("No enciende"))

        avanzar(DictadoViewModel.TOPE_SILENCIO_MS - 1)
        assertEquals(0, reconocedor.detenciones)

        avanzar(1)
        assertEquals(1, reconocedor.detenciones)   // D-10
        reconocedor.emitir(EventoDictado.Final("No enciende"))
        assertFalse(vm.ui.escuchando)
        assertEquals("No enciende", vm.ui.texto)
    }

    @Test
    fun `cada palabra nueva reinicia el tope`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()

        avanzar(DictadoViewModel.TOPE_SILENCIO_MS - 1_000)
        reconocedor.emitir(EventoDictado.Parcial("No enciende"))
        avanzar(DictadoViewModel.TOPE_SILENCIO_MS - 1_000)

        assertEquals(0, reconocedor.detenciones)
    }

    @Test
    fun `seguir dictando despues de detener suma lo nuevo al final`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        vm.detenerCon("No enciende.")

        vm.alSeguirDictando()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("Huele"))
        assertEquals("No enciende. Huele", vm.ui.textoVisible)
        vm.detenerCon("Huele a quemado")

        assertEquals(2, reconocedor.escuchas)
        assertEquals("No enciende. Huele a quemado", vm.ui.texto)   // RF-1008
    }

    @Test
    fun `seguir dictando mientras escucha no abre otra escucha`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        vm.alSeguirDictando()
        assertEquals(1, reconocedor.escuchas)
    }

    @Test
    fun `detener sin haber dicho nada avisa que no se entendio`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()

        vm.alDetener()
        reconocedor.emitir(EventoDictado.Error(ErrorDictado.SIN_ENTENDER))

        assertEquals(ErrorDictado.SIN_ENTENDER, vm.ui.error)   // RF-1015
        assertFalse(vm.ui.escuchando)
    }

    @Test
    fun `volver a escuchar limpia el error`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Error(ErrorDictado.SIN_CONEXION))
        assertEquals(ErrorDictado.SIN_CONEXION, vm.ui.error)   // RF-1016
        assertFalse(vm.ui.escuchando)

        vm.alSeguirDictando()
        assertNull(vm.ui.error)
    }

    @Test
    fun `sin servicio de voz no escucha`() {
        reconocedor.disponible = false
        val vm = nuevoVm()
        vm.alPermisoConcedido()

        assertTrue(vm.ui.sinServicio)   // RF-1017
        assertEquals(0, reconocedor.escuchas)
    }

    @Test
    fun `si el reconocedor avisa que no hay servicio deja de ofrecer el microfono`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Error(ErrorDictado.SIN_SERVICIO))

        assertTrue(vm.ui.sinServicio)
        avanzar(DictadoViewModel.REINICIO_MS)
        vm.alSeguirDictando()
        assertEquals(1, reconocedor.escuchas)
    }

    @Test
    fun `un error del microfono detiene la escucha y conserva el texto`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Parcial("No enciende"), EventoDictado.Error(ErrorDictado.MICROFONO))
        avanzar(DictadoViewModel.REINICIO_MS)

        assertEquals(ErrorDictado.MICROFONO, vm.ui.error)   // RF-1018
        assertEquals("No enciende", vm.ui.texto)
        assertFalse(vm.ui.escuchando)
        assertEquals(1, reconocedor.escuchas)
    }

    @Test
    fun `dejar de ver suelta el microfono y conserva lo parcial`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende"))

        vm.alDejarDeVer()
        avanzar(DictadoViewModel.REINICIO_MS)

        assertEquals(1, reconocedor.cancelaciones)   // RF-1014
        assertFalse(reconocedor.escuchando)
        assertFalse(vm.ui.escuchando)
        assertEquals("No enciende", vm.ui.texto)
        assertEquals(1, reconocedor.escuchas)
    }

    @Test
    fun `usar el texto lo entrega recortado y suelta el microfono`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende"))
        avanzar(DictadoViewModel.REINICIO_MS)
        reconocedor.emitir(EventoDictado.Parcial("  huele a quemado  "))

        vm.alUsarTexto()

        assertEquals("No enciende huele a quemado", vm.ui.textoListo)   // RF-1010
        assertFalse(reconocedor.escuchando)
    }

    @Test
    fun `el texto entregado se consume una sola vez`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        vm.detenerCon("No enciende")
        vm.alUsarTexto()

        vm.alTextoEntregado()

        assertNull(vm.ui.textoListo)
    }
}
