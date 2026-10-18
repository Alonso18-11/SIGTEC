package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.MainDispatcherRule
import com.softcorp.sigtec.feature.captura.data.CatalogoEquiposRepositoryFalso
import com.softcorp.sigtec.feature.captura.domain.LecturaCodigo
import com.softcorp.sigtec.feature.captura.domain.LeerCodigoEquipo
import com.softcorp.sigtec.feature.captura.domain.RachaLectura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** HU-09 (RF-905 a RF-908): cuándo «Escanear etiqueta» da un código por reconocido. */
class EscaneoViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    // Se crea al usarlo, cuando la regla ya reemplazó Dispatchers.Main para viewModelScope
    private val vm by lazy { EscaneoViewModel(LeerCodigoEquipo(CatalogoEquiposRepositoryFalso())) }

    private fun leer(vararg textos: String) = textos.forEach(vm::alLeerTexto)

    @Test
    fun `tres imagenes con el mismo codigo lo reconocen`() {
        leer("PC-CONT-014", "Área: Contabilidad PC-CONT-014", "PC-C0NT-014")
        assertEquals(LecturaCodigo("PC-CONT-014", enCatalogo = true), vm.estado.value.reconocido)
    }

    @Test
    fun `lecturas que alternan no reconocen nada`() {
        leer("PC-CONT-014", "PC-LOG-009", "PC-CONT-014", "PC-LOG-009")
        assertNull(vm.estado.value.reconocido)
        assertEquals(RachaLectura("PC-LOG-009", 1), vm.estado.value.racha)
    }

    @Test
    fun `con un codigo reconocido se ignoran las imagenes nuevas`() {
        leer("PC-CONT-014", "PC-CONT-014", "PC-CONT-014")
        val antes = vm.estado.value
        leer("PC-LOG-009", "PC-LOG-009", "PC-LOG-009")
        assertEquals(antes, vm.estado.value)
    }

    @Test
    fun `volver a escanear descarta el codigo y la racha`() {
        leer("PC-CONT-014", "PC-CONT-014", "PC-CONT-014")
        vm.alVolverAEscanear()
        assertEquals(EscaneoUiState(), vm.estado.value)
        leer("PC-LOG-009", "PC-LOG-009", "PC-LOG-009")
        assertEquals("PC-LOG-009", vm.estado.value.reconocido?.codigo)
    }

    @Test
    fun `un codigo fuera del catalogo se reconoce con el aviso`() {
        leer("LAP-GER-005", "LAP-GER-005", "LAP-GER-005")
        assertEquals(LecturaCodigo("LAP-GER-005", enCatalogo = false), vm.estado.value.reconocido)
    }
}
