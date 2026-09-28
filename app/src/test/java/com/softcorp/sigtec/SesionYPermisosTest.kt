package com.softcorp.sigtec

import com.softcorp.sigtec.core.data.SesionRepositoryDemo
import com.softcorp.sigtec.core.domain.BloqueoApp
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.IniciarSesion
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionYPermisosTest {

    private val sesion = SesionRepositoryDemo()
    private val verificarPermiso = VerificarPermiso(sesion)

    @Test
    fun `sin sesion no hay permiso`() {
        assertEquals(
            Resultado.Error(ErrorDominio.SinPermiso),
            verificarPermiso(Permiso.REGISTRAR_INCIDENCIA)
        )
    }

    @Test
    fun `el tecnico no puede asignar aunque el menu fallara`() = runTest {
        sesion.iniciarSesion("lramirez@softcorp.pe", "")
        assertEquals(
            Resultado.Error(ErrorDominio.SinPermiso),
            verificarPermiso(Permiso.ASIGNAR_INCIDENCIA)
        )
    }

    @Test
    fun `el jefe puede asignar y queda identificado`() = runTest {
        sesion.iniciarSesion("cmendoza@softcorp.pe", "")
        val resultado = verificarPermiso(Permiso.ASIGNAR_INCIDENCIA)
        assertTrue(resultado is Resultado.Exito && resultado.valor.nombre == "Carlos Mendoza")
    }

    @Test
    fun `un correo desconocido es credencial invalida`() = runTest {
        assertEquals(
            Resultado.Error(ErrorDominio.CredencialesInvalidas),
            IniciarSesion(sesion, BloqueoApp())("nadie@softcorp.pe", "x")
        )
    }
}