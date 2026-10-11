package com.softcorp.sigtec

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.domain.FiltrosIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.SIN_TECNICO
import com.softcorp.sigtec.feature.incidencias.domain.filtrar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** RF-43/RF-44: función pura, sin ViewModel ni StateFlow. */
class FiltrarIncidenciasTest {

    private val ahora = Instant.parse("2026-09-27T15:00:00Z")

    private fun incidencia(
        id: String, numero: Int?, equipo: String, descripcion: String,
        marca: MarcaSeguimiento, tecnico: String?, horasAtras: Long
    ) = Incidencia(
        id = id, numero = numero, codigoEquipo = equipo, descripcion = descripcion,
        usuarioResponsable = "María Quispe", fechaRegistro = ahora.minus(Duration.ofHours(horasAtras)),
        registradoPorId = "t1", registradoPorNombre = "Luis Ramírez", marca = marca,
        tecnicoAsignadoId = tecnico?.let { "id-$it" }, tecnicoAsignadoNombre = tecnico
    )

    // Mismos datos que usaba ListaIncidenciasViewModelTest antes de mover estos casos
    private val datos = listOf(
        incidencia("a", 119, "PC-ADM-022", "Teclas del teclado que no responden", MarcaSeguimiento.CERRADA, "Luis Ramírez", 170),
        incidencia("b", null, "IMP-RRHH-002", "La impresora no aparece en la red", MarcaSeguimiento.SIN_ASIGNAR, null, 1),
        incidencia("c", 124, "PC-CONT-014", "No enciende después del corte de luz", MarcaSeguimiento.EN_ATENCION, "Luis Ramírez", 3),
        incidencia("d", 126, "PC-VENT-031", "Pantalla azul al iniciar Windows", MarcaSeguimiento.ESPERANDO_REPUESTO, "Ana Torres", 2),
        incidencia("e", 121, "PC-CONT-014", "Sin conexión a la red Wi-Fi", MarcaSeguimiento.ASIGNADA, "Ana Torres", 150)
    )

    private fun List<Incidencia>.idsFiltrando(f: FiltrosIncidencias): List<String> = filtrar(f).map { it.id }

    @Test
    fun `busca por codigo INC, con o sin el prefijo`() {
        assertEquals(listOf("c"), datos.idsFiltrando(FiltrosIncidencias(texto = "inc-0124")))
        assertEquals(listOf("d"), datos.idsFiltrando(FiltrosIncidencias(texto = " 126 ")))
    }

    @Test
    fun `busca por equipo sin importar mayusculas`() {
        assertEquals(listOf("c", "e"), datos.idsFiltrando(FiltrosIncidencias(texto = "pc-cont")))
    }

    @Test
    fun `busca por problema sin importar tildes`() {
        assertEquals(listOf("c"), datos.idsFiltrando(FiltrosIncidencias(texto = "despues del CORTE")))
        assertEquals(listOf("e"), datos.idsFiltrando(FiltrosIncidencias(texto = "conexion")))
    }

    @Test
    fun `filtra por cada marca de seguimiento`() {
        val esperado = mapOf(
            MarcaSeguimiento.SIN_ASIGNAR to listOf("b"),
            MarcaSeguimiento.ASIGNADA to listOf("e"),
            MarcaSeguimiento.EN_ATENCION to listOf("c"),
            MarcaSeguimiento.ESPERANDO_REPUESTO to listOf("d"),
            MarcaSeguimiento.CERRADA to listOf("a")
        )
        esperado.forEach { (marca, ids) ->
            assertEquals("marca $marca", ids, datos.idsFiltrando(FiltrosIncidencias(marca = marca)))
        }
    }

    @Test
    fun `filtra por tecnico y por sin tecnico`() {
        assertEquals(listOf("d", "e"), datos.idsFiltrando(FiltrosIncidencias(tecnico = "Ana Torres")))
        assertEquals(listOf("b"), datos.idsFiltrando(FiltrosIncidencias(tecnico = SIN_TECNICO)))
    }

    @Test
    fun `filtra por equipo`() {
        assertEquals(listOf("c", "e"), datos.idsFiltrando(FiltrosIncidencias(equipo = "PC-CONT-014")))
    }

    @Test
    fun `combina busqueda y filtros`() {
        assertEquals(
            listOf("e"),
            datos.idsFiltrando(FiltrosIncidencias(equipo = "PC-CONT-014", tecnico = "Ana Torres"))
        )
        assertTrue(
            datos.idsFiltrando(
                FiltrosIncidencias(equipo = "PC-CONT-014", tecnico = "Ana Torres", texto = "enciende")
            ).isEmpty()
        )
    }
}
