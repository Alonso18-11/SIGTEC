package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** HU-16 (D-2): la ventana de 3 meses y el umbral de «Equipo crítico», compartidos con el tablero (RF-1521). */
class ReglasEquipoTest {

    private val ahora = Instant.parse("2026-11-16T15:00:00Z")

    private fun registradaHace(dias: Long, segundos: Long = 0) = Incidencia(
        id = "i", numero = 1, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe",
        fechaRegistro = ahora.minus(Duration.ofDays(dias)).minusSeconds(segundos),
        registradoPorId = "u", registradoPorNombre = "Carlos Mendoza", marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `cuentan las registradas hoy y hasta justo 90 dias atras`() {
        assertTrue(registradaEnTresMeses(registradaHace(0), ahora))
        assertTrue(registradaEnTresMeses(registradaHace(90), ahora))
        assertFalse(registradaEnTresMeses(registradaHace(90, segundos = 1), ahora))
    }

    @Test
    fun `es critico desde tres incidencias en tres meses`() {
        assertFalse(esEquipoCritico(0))
        assertFalse(esEquipoCritico(2))
        assertTrue(esEquipoCritico(3))
        assertTrue(esEquipoCritico(7))
    }
}
