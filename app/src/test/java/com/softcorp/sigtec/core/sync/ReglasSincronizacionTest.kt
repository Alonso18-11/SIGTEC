package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
