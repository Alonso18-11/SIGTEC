package com.softcorp.sigtec.feature.captura.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-09 (RF-916 a RF-920, RF-922) y HU-10 (RF-1019 a RF-1023): qué dibuja una pantalla según el permiso. */
class PermisoRecursoTest {

    @Test
    fun `sin el recurso no ofrece el permiso aunque este concedido`() {
        assertEquals(VistaPermiso.SIN_RECURSO, vistaPermiso(tieneRecurso = false, concedido = true, yaSePidio = false, androidLoExplicaria = false))
        assertEquals(VistaPermiso.SIN_RECURSO, vistaPermiso(tieneRecurso = false, concedido = false, yaSePidio = true, androidLoExplicaria = true))
    }

    @Test
    fun `concedido muestra el contenido`() {
        assertEquals(VistaPermiso.CONCEDIDO, vistaPermiso(tieneRecurso = true, concedido = true, yaSePidio = true, androidLoExplicaria = false))
    }

    @Test
    fun `sin pedirlo todavia explica para que se usa`() {
        assertEquals(VistaPermiso.EXPLICAR, vistaPermiso(tieneRecurso = true, concedido = false, yaSePidio = false, androidLoExplicaria = false))
    }

    @Test
    fun `negado con explicacion de Android vuelve a ofrecer el permiso`() {
        assertEquals(VistaPermiso.NEGADO, vistaPermiso(tieneRecurso = true, concedido = false, yaSePidio = true, androidLoExplicaria = true))
        // Al volver a abrir la pantalla, Android recuerda la negativa aunque esta vez no se haya pedido
        assertEquals(VistaPermiso.NEGADO, vistaPermiso(tieneRecurso = true, concedido = false, yaSePidio = false, androidLoExplicaria = true))
    }

    @Test
    fun `negado sin explicacion despues de pedirlo manda a los ajustes`() {
        assertEquals(VistaPermiso.IR_A_AJUSTES, vistaPermiso(tieneRecurso = true, concedido = false, yaSePidio = true, androidLoExplicaria = false))
    }

    @Test
    fun `quitado desde los ajustes vuelve a la explicacion inicial`() {
        // RF-922: al quitarlo desde los ajustes, Android olvida las negativas y nadie lo ha pedido en esta pantalla
        assertEquals(VistaPermiso.EXPLICAR, vistaPermiso(tieneRecurso = true, concedido = false, yaSePidio = false, androidLoExplicaria = false))
    }
}
