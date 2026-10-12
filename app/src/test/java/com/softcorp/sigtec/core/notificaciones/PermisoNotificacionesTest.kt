package com.softcorp.sigtec.core.notificaciones

import com.softcorp.sigtec.core.domain.model.Perfil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-07 (RF-708, RF-710): a quién y cuándo se le pide el permiso de notificaciones. */
class PermisoNotificacionesTest {

    @Test
    fun `tecnico en Android 13 sin permiso se le pide`() {
        assertTrue(debePedirPermisoNotificaciones(sdk = 33, concedido = false, perfil = Perfil.TECNICO))
    }

    @Test
    fun `en Android 12 no se pide porque se concede al instalar`() {
        assertFalse(debePedirPermisoNotificaciones(sdk = 32, concedido = false, perfil = Perfil.TECNICO))
    }

    @Test
    fun `si ya esta concedido no se vuelve a pedir`() {
        assertFalse(debePedirPermisoNotificaciones(sdk = 34, concedido = true, perfil = Perfil.TECNICO))
    }

    @Test
    fun `al jefe y al personal de sistemas no se les pide porque no reciben asignaciones`() {
        assertFalse(debePedirPermisoNotificaciones(sdk = 34, concedido = false, perfil = Perfil.JEFE))
        assertFalse(debePedirPermisoNotificaciones(sdk = 34, concedido = false, perfil = Perfil.PERSONAL_SISTEMAS))
    }

    @Test
    fun `sin perfil no se pide`() {
        assertFalse(debePedirPermisoNotificaciones(sdk = 34, concedido = false, perfil = null))
    }
}
