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
    fun `una incidencia nueva que baja no queda marcada`() {
        assertFalse(atencionAlBajar(null, incidencia(false, antes, "remota")))
    }

    @Test
    fun `lo ya sincronizado se reemplaza sin marca`() {
        assertFalse(atencionAlBajar(incidencia(false, antes, "local"), incidencia(false, despues, "remota")))
    }

    @Test
    fun `un cambio local sin subir que pierde frente a una remota mas reciente queda marcado`() {
        assertTrue(atencionAlBajar(incidencia(true, antes, "local"), incidencia(false, despues, "remota")))
    }

    @Test
    fun `un cambio local sin subir no se marca si la remota no es mas reciente`() {
        assertFalse(atencionAlBajar(incidencia(true, despues, "local"), incidencia(false, antes, "remota")))
    }

    @Test
    fun `una incidencia ya rechazada conserva la marca al bajar la version remota`() {
        val rechazada = incidencia(false, despues, "local").copy(necesitaAtencion = true)
        assertTrue(atencionAlBajar(rechazada, incidencia(false, antes, "remota")))
    }

    @Test
    fun `si el tecnico no cambia la carga no se mueve`() {
        assertEquals(emptyMap<String, Int>(), cambiosDeCarga("u-luis", "u-luis"))
        assertEquals(emptyMap<String, Int>(), cambiosDeCarga(null, null))
    }

    @Test
    fun `asignar suma 1 al tecnico nuevo`() {
        assertEquals(mapOf("u-luis" to 1), cambiosDeCarga(null, "u-luis"))
    }

    @Test
    fun `reasignar resta 1 al anterior y suma 1 al nuevo`() {
        assertEquals(mapOf("u-ana" to -1, "u-luis" to 1), cambiosDeCarga("u-ana", "u-luis"))
    }

    @Test
    fun `quitar el tecnico resta 1 al anterior`() {
        assertEquals(mapOf("u-ana" to -1), cambiosDeCarga("u-ana", null))
    }

    @Test
    fun `la carga puede subir hasta llegar justo al limite`() {
        assertTrue(cargaValida(actual = 3, limite = 5, delta = 1))
        assertTrue(cargaValida(actual = 4, limite = 5, delta = 1))
    }

    @Test
    fun `la carga no puede pasar del limite`() {
        // RF-621: otro celular ya ocupó el último cupo
        assertFalse(cargaValida(actual = 5, limite = 5, delta = 1))
    }

    @Test
    fun `la carga no puede quedar bajo cero`() {
        assertTrue(cargaValida(actual = 1, limite = 5, delta = -1))
        assertFalse(cargaValida(actual = 0, limite = 5, delta = -1))
    }

    @Test
    fun `sin cupo definido nunca se puede asignar`() {
        // Perfil sin limiteAtencion: se lee como 0
        assertFalse(cargaValida(actual = 0, limite = 0, delta = 1))
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

    // ---------- HU-07: asignación nueva para quien tiene la sesión (RF-702, RF-703, RF-705) ----------

    private fun asignada(tecnico: String?, marca: MarcaSeguimiento = MarcaSeguimiento.ASIGNADA) =
        incidencia(false, antes, "remota").copy(
            tecnicoAsignadoId = tecnico, tecnicoAsignadoNombre = tecnico, marca = marca
        )

    @Test
    fun `asignada antes a otro o sin tecnico y ahora a mi avisa`() {
        assertTrue(esAsignacionNuevaPara("luis", local = asignada(null, MarcaSeguimiento.SIN_ASIGNAR), remota = asignada("luis")))
        assertTrue(esAsignacionNuevaPara("luis", local = asignada("ana"), remota = asignada("luis")))
    }

    @Test
    fun `si llega por primera vez ya asignada a mi no avisa`() {
        assertFalse(esAsignacionNuevaPara("luis", local = null, remota = asignada("luis")))
    }

    @Test
    fun `si ya era mia no avisa otra vez`() {
        assertFalse(esAsignacionNuevaPara("luis", local = asignada("luis"), remota = asignada("luis", MarcaSeguimiento.EN_ATENCION)))
    }

    @Test
    fun `asignada a mi pero cerrada no avisa`() {
        assertFalse(esAsignacionNuevaPara("luis", local = asignada("ana"), remota = asignada("luis", MarcaSeguimiento.CERRADA)))
    }

    @Test
    fun `reasignada de mi a otra persona no avisa`() {
        assertFalse(esAsignacionNuevaPara("luis", local = asignada("luis"), remota = asignada("ana")))
    }
}
