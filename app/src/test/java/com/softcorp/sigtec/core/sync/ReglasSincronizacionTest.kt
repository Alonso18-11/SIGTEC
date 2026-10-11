package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class ReglasSincronizacionTest {

    private val antes = Instant.parse("2026-09-28T10:00:00Z")
    private val despues = Instant.parse("2026-09-28T10:05:00Z")

    private fun incidencia(pendiente: Boolean, actualizadoEn: Instant, descripcion: String) =
        IncidenciaEntity(
            id = "i1", numero = 1, codigoEquipo = "PC-CONT-014", descripcion = descripcion,
            usuarioResponsable = "María Quispe", fechaRegistro = antes, registradoPorId = "t1",
            registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.EN_ATENCION,
            tipoFalla = null, prioridad = null, tecnicoAsignadoId = "t1",
            tecnicoAsignadoNombre = "Luis Ramírez", critica = false,
            pendienteSincronizar = pendiente, actualizadoEn = actualizadoEn
        )

    private fun movimiento(pendiente: Boolean) = MovimientoEntity(
        id = "m1", incidenciaId = "i1", tipo = TipoMovimiento.REGISTRADA,
        autorNombre = "Luis Ramírez", fecha = antes, detalle = null, pendienteSincronizar = pendiente
    )

    @Test
    fun `una incidencia nueva siempre se guarda`() {
        assertTrue(debeReemplazarLocal(null, incidencia(false, antes, "remota")))
    }

    @Test
    fun `lo ya sincronizado se actualiza con la version remota`() {
        assertTrue(debeReemplazarLocal(incidencia(false, antes, "local"), incidencia(false, antes, "remota")))
    }

    @Test
    fun `un cambio local sin subir no se pisa con una version remota mas antigua`() {
        assertFalse(debeReemplazarLocal(incidencia(true, despues, "local"), incidencia(false, antes, "remota")))
    }

    @Test
    fun `un cambio remoto mas reciente gana aunque haya un cambio local sin subir`() {
        assertTrue(debeReemplazarLocal(incidencia(true, antes, "local"), incidencia(false, despues, "remota")))
    }

    @Test
    fun `un movimiento que falta se guarda y uno pendiente no se pisa`() {
        assertTrue(debeGuardarMovimiento(null))
        assertTrue(debeGuardarMovimiento(movimiento(pendiente = false)))
        assertFalse(debeGuardarMovimiento(movimiento(pendiente = true)))
    }

    @Test
    fun `sin contador previo el siguiente numero es 1`() {
        assertEquals(1L, siguienteNumero(null))
    }

    @Test
    fun `con contador en 0 el siguiente numero es 1`() {
        assertEquals(1L, siguienteNumero(0L))
    }

    @Test
    fun `con contador en 5 el siguiente numero es 6`() {
        assertEquals(6L, siguienteNumero(5L))
    }

    @Test
    fun `permiso denegado es un error permanente`() {
        assertTrue(esCodigoPermanente("PERMISSION_DENIED"))
    }

    @Test
    fun `precondicion fallida no es un error permanente porque en una transaccion se reintenta`() {
        assertFalse(esCodigoPermanente("FAILED_PRECONDITION"))
    }

    @Test
    fun `argumento invalido es un error permanente`() {
        assertTrue(esCodigoPermanente("INVALID_ARGUMENT"))
    }

    @Test
    fun `servicio no disponible no es un error permanente`() {
        assertFalse(esCodigoPermanente("UNAVAILABLE"))
    }

    @Test
    fun `tiempo de espera agotado no es un error permanente`() {
        assertFalse(esCodigoPermanente("DEADLINE_EXCEEDED"))
    }

    @Test
    fun `una excepcion que no es de firestore no es un error permanente`() {
        assertFalse(esErrorPermanente(IOException("sin conexion")))
    }

    @Test
    fun `con conexion validada se debe intentar subir`() {
        assertTrue(debeIntentarSubir(hayConexionValidada = true))
    }

    @Test
    fun `sin conexion validada no se debe intentar subir`() {
        assertFalse(debeIntentarSubir(hayConexionValidada = false))
    }
}
