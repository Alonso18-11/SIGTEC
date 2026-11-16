package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** HU-14 (RF-1434, RF-1436): el orden «Más urgentes» de la lista de incidencias. */
class OrdenIncidenciasTest {

    private val base = Instant.parse("2026-11-16T12:00:00Z")

    private fun inc(id: String, prioridad: Prioridad?, horasAtras: Long, marca: MarcaSeguimiento = MarcaSeguimiento.ASIGNADA) =
        Incidencia(
            id = id, numero = null, codigoEquipo = "PC-CONT-014", descripcion = "Falla $id", usuarioResponsable = "María",
            fechaRegistro = base.minusSeconds(3600 * horasAtras), registradoPorId = "u", registradoPorNombre = "Luis",
            marca = marca, prioridad = prioridad
        )

    @Test
    fun `mas recientes deja la lista como llega`() {
        val lista = listOf(inc("a", null, 1), inc("b", Prioridad.ALTA, 5))
        assertEquals(lista, lista.ordenar(OrdenIncidencias.RECIENTES))
    }

    @Test
    fun `primero alta, luego media, baja y sin prioridad`() {
        val lista = listOf(inc("sin", null, 1), inc("baja", Prioridad.BAJA, 1), inc("alta", Prioridad.ALTA, 1), inc("media", Prioridad.MEDIA, 1))
        assertEquals(listOf("alta", "media", "baja", "sin"), lista.ordenar(OrdenIncidencias.URGENTES).map { it.id })
    }

    @Test
    fun `con la misma prioridad va primero la mas antigua`() {
        val lista = listOf(inc("nueva", Prioridad.ALTA, 1), inc("vieja", Prioridad.ALTA, 30))
        assertEquals(listOf("vieja", "nueva"), lista.ordenar(OrdenIncidencias.URGENTES).map { it.id })
    }

    @Test
    fun `las solucionadas van al final, la mas reciente primero`() {
        val lista = listOf(
            inc("cerrada-vieja", Prioridad.ALTA, 50, MarcaSeguimiento.CERRADA),
            inc("pendiente", null, 1, MarcaSeguimiento.SIN_ASIGNAR),
            inc("cerrada-nueva", Prioridad.ALTA, 2, MarcaSeguimiento.CERRADA)
        )
        assertEquals(
            listOf("pendiente", "cerrada-nueva", "cerrada-vieja"),
            lista.ordenar(OrdenIncidencias.URGENTES).map { it.id }
        )
    }
}
