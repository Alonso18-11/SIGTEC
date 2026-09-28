package com.softcorp.sigtec

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.usecase.BloquearApp
import com.softcorp.sigtec.core.domain.usecase.CambiarIngresoConHuella
import com.softcorp.sigtec.core.domain.usecase.CerrarSesion
import com.softcorp.sigtec.core.domain.usecase.DesbloquearApp
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoAcceso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private class PreferenciasFalsas : PreferenciasSeguridadRepository {
    private val estado = MutableStateFlow(PreferenciasHuella())
    override val preferenciasHuella: Flow<PreferenciasHuella> = estado
    override suspend fun asignarPropietario(id: String, nombre: String) =
        estado.update { PreferenciasHuella(id, nombre, ofrecida = true) }
    override suspend fun quitarPropietario() =
        estado.update { it.copy(propietarioId = null, propietarioNombre = null) }
    override suspend fun marcarOfrecida() = estado.update { it.copy(ofrecida = true) }
    override suspend fun limpiar() = estado.update { PreferenciasHuella() }
}

class EstadoAccesoTest {

    private val sesion = SesionRepositoryDemo()
    private val preferencias = PreferenciasFalsas()
    private val bloqueo = BloqueoApp()

    private val estado = ObservarEstadoAcceso(sesion, preferencias, bloqueo)
    private val cambiarHuella = CambiarIngresoConHuella(sesion, preferencias, bloqueo)
    private val cerrarSesion = CerrarSesion(sesion, preferencias, bloqueo)

    // sesion.iniciarSesion directo = sesión restaurada al abrir la app (no desbloquea)
    private suspend fun entra(correo: String) = sesion.iniciarSesion(correo, "")
    private suspend fun luisActivaSuHuella() {
        entra("lramirez@softcorp.pe")
        cambiarHuella(true)
        bloqueo.bloquear()   // simula cerrar y abrir la app
    }

    @Test
    fun `sin huella vinculada la sesion restaurada entra directo`() = runTest {
        entra("lramirez@softcorp.pe")
        assertTrue(estado().first() is EstadoAcceso.Activa)
    }

    @Test
    fun `el dueno queda bloqueado hasta usar su huella`() = runTest {
        luisActivaSuHuella()
        assertTrue(estado().first() is EstadoAcceso.Bloqueada)
        DesbloquearApp(bloqueo)()
        assertTrue(estado().first() is EstadoAcceso.Activa)
    }

    @Test
    fun `activar la huella no expulsa a quien la esta activando`() = runTest {
        entra("lramirez@softcorp.pe")
        cambiarHuella(true)
        assertTrue(estado().first() is EstadoAcceso.Activa)
    }

    @Test
    fun `bloquear conserva la sesion del dueno`() = runTest {
        entra("lramirez@softcorp.pe")
        cambiarHuella(true)
        BloquearApp(bloqueo)()
        val actual = estado().first()
        assertTrue(actual is EstadoAcceso.Bloqueada && actual.usuario.nombre == "Luis Ramírez")
    }

    @Test
    fun `otra persona en el celular entra sin huella`() = runTest {
        luisActivaSuHuella()
        entra("atorres@softcorp.pe")
        assertTrue(estado().first() is EstadoAcceso.Activa)
    }

    @Test
    fun `otra persona no puede desactivar ni tomar la huella del dueno`() = runTest {
        luisActivaSuHuella()
        entra("atorres@softcorp.pe")
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), cambiarHuella(false))
        assertEquals(Resultado.Error(ErrorDominio.SinPermiso), cambiarHuella(true))
        assertEquals("Luis Ramírez", preferencias.preferenciasHuella.first().propietarioNombre)
    }

    @Test
    fun `si otra persona cierra sesion la huella del dueno se conserva`() = runTest {
        luisActivaSuHuella()
        entra("atorres@softcorp.pe")
        cerrarSesion()
        assertEquals("Luis Ramírez", preferencias.preferenciasHuella.first().propietarioNombre)
        assertTrue(estado().first() is EstadoAcceso.SinSesion)
    }

    @Test
    fun `si el dueno cierra sesion se desvincula su huella`() = runTest {
        entra("lramirez@softcorp.pe")
        cambiarHuella(true)
        cerrarSesion()
        assertFalse(preferencias.preferenciasHuella.first().activada)
    }
}