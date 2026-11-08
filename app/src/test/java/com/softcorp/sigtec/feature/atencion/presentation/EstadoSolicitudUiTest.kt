package com.softcorp.sigtec.feature.atencion.presentation

import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-11 (RF-1120): cada estado de la solicitud tiene su texto. */
class EstadoSolicitudUiTest {

    @Test
    fun `cada estado tiene el texto de la spec`() {
        assertEquals("Por enviar a Logística", textoEstadoSolicitud(EstadoSolicitudVisible.POR_ENVIAR))
        assertEquals("Por despachar", textoEstadoSolicitud(EstadoSolicitudVisible.POR_DESPACHAR))
        assertEquals("En camino", textoEstadoSolicitud(EstadoSolicitudVisible.EN_CAMINO))
        assertEquals("Entregado", textoEstadoSolicitud(EstadoSolicitudVisible.ENTREGADA))
        assertEquals("Rechazada", textoEstadoSolicitud(EstadoSolicitudVisible.RECHAZADA))
    }
}
