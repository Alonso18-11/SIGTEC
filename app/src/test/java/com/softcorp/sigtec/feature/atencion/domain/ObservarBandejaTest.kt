package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** HU-08 (RF-801, RF-803, RF-805): la bandeja del técnico con sesión actual. */
class ObservarBandejaTest {

    private val sesion = SesionRepositoryDemo()
    private val repo = AtencionRepositoryFalso()
    private val observarBandeja = ObservarBandeja(ObservarUsuarioActual(sesion), repo)

    // INC-0125: la más reciente de las dos
    private val masReciente = Incidencia(
        id = "i125", numero = 125, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-12T08:00:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.ASIGNADA, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    // INC-0124: la más antigua de las dos
    private val masAntigua = Incidencia(
        id = "i124", numero = 124, codigoEquipo = "IMP-RRHH-002", descripcion = "La impresora no aparece en la red",
        usuarioResponsable = "José Díaz", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
        registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
        marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
    )

    @Test
    fun `la bandeja queda ordenada de la mas antigua a la mas reciente`() = runTest {
        // RF-801
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(listOf(masReciente, masAntigua))

        val bandeja = observarBandeja().first()

        assertEquals(listOf(masAntigua, masReciente), bandeja)
    }

    @Test
    fun `sin pendientes la bandeja queda vacia`() = runTest {
        // RF-803
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        repo.cargar(emptyList())

        val bandeja = observarBandeja().first()

        assertTrue(bandeja.isEmpty())
    }

    @Test
    fun `sin sesion la bandeja queda vacia`() = runTest {
        repo.cargar(listOf(masReciente, masAntigua))

        val bandeja = observarBandeja().first()

        assertTrue(bandeja.isEmpty())
    }
}
