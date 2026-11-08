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

/** HU-10 (RF-1004 a RF-1018): escuchar, detener, seguir sumando, los errores y entregar el texto. */
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

        reconocedor.emitir(EventoDictado.Final("No enciende"))
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
    fun `detener le pide al reconocedor que entregue lo reconocido`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende"))

        vm.alDetener()
        reconocedor.emitir(EventoDictado.Final("No enciende"))

        assertEquals(1, reconocedor.detenciones)
        assertEquals("No enciende", vm.ui.texto)
        assertEquals("", vm.ui.parcial)
        assertFalse(vm.ui.escuchando)
    }

    @Test
    fun `seguir dictando suma lo nuevo al final`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende."))

        vm.alSeguirDictando()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("Huele"))
        assertEquals("No enciende. Huele", vm.ui.textoVisible)
        reconocedor.emitir(EventoDictado.Final("Huele a quemado"))

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
    fun `si no se entiende conserva el texto y muestra el error`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende"))
        vm.alSeguirDictando()
        reconocedor.emitir(EventoDictado.Parcial("eh"), EventoDictado.Error(ErrorDictado.SIN_ENTENDER))

        assertEquals(ErrorDictado.SIN_ENTENDER, vm.ui.error)   // RF-1015
        assertEquals("No enciende eh", vm.ui.texto)
        assertFalse(vm.ui.escuchando)
    }

    @Test
    fun `volver a escuchar limpia el error`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Error(ErrorDictado.SIN_CONEXION))
        assertEquals(ErrorDictado.SIN_CONEXION, vm.ui.error)   // RF-1016

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
        vm.alSeguirDictando()
        assertEquals(1, reconocedor.escuchas)
    }

    @Test
    fun `un error del microfono conserva el texto`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Parcial("No enciende"), EventoDictado.Error(ErrorDictado.MICROFONO))

        assertEquals(ErrorDictado.MICROFONO, vm.ui.error)   // RF-1018
        assertEquals("No enciende", vm.ui.texto)
    }

    @Test
    fun `dejar de ver suelta el microfono y conserva lo parcial`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Escuchando, EventoDictado.Parcial("No enciende"))

        vm.alDejarDeVer()

        assertEquals(1, reconocedor.cancelaciones)   // RF-1014
        assertFalse(reconocedor.escuchando)
        assertFalse(vm.ui.escuchando)
        assertEquals("No enciende", vm.ui.texto)
    }

    @Test
    fun `usar el texto lo entrega recortado y suelta el microfono`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende"))
        vm.alSeguirDictando()
        reconocedor.emitir(EventoDictado.Parcial("  huele a quemado  "))

        vm.alUsarTexto()

        assertEquals("No enciende huele a quemado", vm.ui.textoListo)   // RF-1010
        assertFalse(reconocedor.escuchando)
    }

    @Test
    fun `el texto entregado se consume una sola vez`() {
        val vm = nuevoVm()
        vm.alPermisoConcedido()
        reconocedor.emitir(EventoDictado.Final("No enciende"))
        vm.alUsarTexto()

        vm.alTextoEntregado()

        assertNull(vm.ui.textoListo)
    }
}
