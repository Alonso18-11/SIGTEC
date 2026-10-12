package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** HU-08 (RF-827 a RF-829): el detalle también observa el informe técnico de la incidencia. */
class ObservarDetalleIncidenciaTest {

    private val repo = IncidenciaRepositoryFalso()
    private val observarDetalle = ObservarDetalleIncidencia(repo)

    // INC-0124: cerrada, con su informe ya en Room
    private val cerrada = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.CERRADA, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    private val informe = InformeTecnico(
        id = "inf1", incidenciaId = "i124", diagnostico = "Fuente de poder quemada",
        trabajoRealizado = "Se reemplazó la fuente de poder", piezasCambiadas = "Fuente ATX 500W",
        observaciones = null, fechaCierre = Instant.parse("2026-10-12T09:15:00Z"),
        confirmadoEn = Instant.parse("2026-10-12T09:15:00Z")
    )

    @Test
    fun `una incidencia con informe lo incluye en el detalle`() = runTest {
        repo.cargar(listOf(cerrada), informes = listOf(informe))
        val detalle = observarDetalle("i124").first()
        assertEquals(informe, detalle?.informe)
    }

    @Test
    fun `una incidencia sin informe muestra el campo en null`() = runTest {
        repo.cargar(listOf(cerrada))
        val detalle = observarDetalle("i124").first()
        assertNull(detalle?.informe)
    }

    @Test
    fun `una incidencia inexistente emite null completo, no solo el informe`() = runTest {
        repo.cargar(listOf(cerrada), informes = listOf(informe))
        val detalle = observarDetalle("no-existe").first()
        assertNull(detalle)
    }
}
