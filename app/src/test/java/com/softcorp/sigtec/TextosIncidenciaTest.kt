package com.softcorp.sigtec

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.feature.incidencias.presentation.codigoParaMostrar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TextosIncidenciaTest {

    private val sinSincronizar = Incidencia(
        id = "i1", numero = null, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = Instant.now(),
        registradoPorId = "t1", registradoPorNombre = "Luis Ramírez",
        marca = MarcaSeguimiento.SIN_ASIGNAR
    )

    @Test
    fun `sin numero se muestra por sincronizar`() {
        assertEquals("Por sincronizar", sinSincronizar.codigoParaMostrar())
        assertEquals("INC-0124", sinSincronizar.copy(numero = 124).codigoParaMostrar())
    }
}
