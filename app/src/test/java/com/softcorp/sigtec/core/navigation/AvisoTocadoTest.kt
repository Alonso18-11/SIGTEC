package com.softcorp.sigtec.core.navigation

import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-07 (RF-711 a RF-713): adónde lleva tocar el aviso según la sesión. */
class AvisoTocadoTest {

    private fun usuario(id: String) =
        Usuario(id, "Luis Ramírez", "lramirez@softcorp.pe", "Soporte y Sistemas", "Técnico", Perfil.TECNICO)

    private val aviso = AvisoTocado(incidenciaId = "i127", usuarioId = "luis")

    @Test
    fun `con la sesion activa de quien recibio el aviso abre el detalle`() {
        assertEquals(DestinoDelAviso.Abrir("i127"), destinoDelAviso(EstadoAcceso.Activa(usuario("luis")), aviso))
    }

    @Test
    fun `con la sesion de otra persona se descarta`() {
        assertEquals(DestinoDelAviso.Descartar, destinoDelAviso(EstadoAcceso.Activa(usuario("ana")), aviso))
    }

    @Test
    fun `con la sesion bloqueada espera al desbloqueo`() {
        assertEquals(DestinoDelAviso.Esperar, destinoDelAviso(EstadoAcceso.Bloqueada(usuario("luis")), aviso))
    }

    @Test
    fun `sin sesion espera a saber quien entra`() {
        assertEquals(DestinoDelAviso.Esperar, destinoDelAviso(EstadoAcceso.SinSesion, aviso))
    }
}
