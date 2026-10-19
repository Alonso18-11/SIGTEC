package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * HU-08 (RF-827 a RF-829): el detalle también observa el informe técnico de la incidencia.
 * HU-09 (RF-932, RF-936): y sus fotos en este celular.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    // HU-09

    private fun foto(id: String, minuto: Long, tipo: TipoEvidencia = TipoEvidencia.FALLA) =
        Evidencia(id, "i124", tipo, "/evidencias/i124/$id.jpg", Instant.parse("2026-10-12T09:00:00Z").plusSeconds(minuto * 60))

    @Test
    fun `las fotos llegan al detalle de la mas antigua a la mas reciente`() = runTest {
        val primera = foto("a", 1)
        val segunda = foto("b", 2, TipoEvidencia.PIEZA_CAMBIADA)
        val deOtra = foto("c", 3).copy(incidenciaId = "otra")
        repo.cargar(listOf(cerrada), evidencias = listOf(segunda, deOtra, primera))

        val detalle = observarDetalle("i124").first()

        assertEquals(listOf(primera, segunda), detalle?.evidencias)
    }

    @Test
    fun `sin fotos en este celular la lista queda vacia`() = runTest {
        repo.cargar(listOf(cerrada))

        assertEquals(emptyList<Evidencia>(), observarDetalle("i124").first()?.evidencias)
    }

    @Test
    fun `una foto nueva aparece sin volver a abrir el detalle`() = runTest {
        repo.cargar(listOf(cerrada))
        val vistos = mutableListOf<DetalleIncidencia?>()
        observarDetalle("i124").onEach { vistos += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        repo.cargar(listOf(cerrada), evidencias = listOf(foto("a", 1)))

        assertEquals(emptyList<Evidencia>(), vistos.first()?.evidencias)
        assertEquals(listOf(foto("a", 1)), vistos.last()?.evidencias)   // RF-936
    }
}
