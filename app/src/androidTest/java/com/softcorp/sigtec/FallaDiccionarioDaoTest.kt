package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.FallaDiccionarioDao
import com.softcorp.sigtec.core.database.entity.FallaDiccionarioEntity
import com.softcorp.sigtec.core.domain.model.TipoFalla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-13: el diccionario sale por título (RF-1301), sabe qué incidencia ya se guardó (RF-1319) y cuenta lo sin subir (RF-1331). */
@RunWith(AndroidJUnit4::class)
class FallaDiccionarioDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: FallaDiccionarioDao

    private val ahora = Instant.parse("2026-11-09T10:00:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.fallaDiccionarioDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun las_fallas_salen_ordenadas_por_titulo_con_todos_sus_campos() = runTest {
        dao.guardar(falla("b", "Pantalla azul al iniciar"))
        dao.guardar(falla("a", "Fuente de poder dañada", origen = "inc-1"))

        val todas = dao.observarTodas().first()

        assertEquals(listOf("a", "b"), todas.map { it.id })
        assertEquals("inc-1", todas.first().incidenciaOrigenId)
        assertEquals(ahora, todas.first().actualizadoEn)
        assertEquals("u-luis", todas.first().autorId)
    }

    @Test
    fun existe_con_origen_solo_para_la_incidencia_guardada() = runTest {
        dao.guardar(falla("a", "Fuente de poder dañada", origen = "inc-1"))

        assertTrue(dao.existeConOrigen("inc-1").first())
        assertFalse(dao.existeConOrigen("inc-2").first())
    }

    @Test
    fun pendientes_se_cuentan_y_dejan_de_contar_al_sincronizar() = runTest {
        dao.guardar(falla("a", "Fuente de poder dañada", pendiente = true))
        dao.guardar(falla("b", "Pantalla azul al iniciar", pendiente = false))

        assertEquals(listOf("a"), dao.pendientesDeSincronizar().map { it.id })
        assertEquals(1, dao.observarCantidadPendientes().first())

        dao.marcarSincronizada("a")

        assertEquals(0, dao.observarCantidadPendientes().first())
        assertEquals(false, dao.porId("a")?.pendienteSincronizar)
    }

    @Test
    fun borrar_sincronizada_no_toca_una_falla_sin_subir() = runTest {
        dao.guardar(falla("a", "Fuente de poder dañada", pendiente = false))
        dao.guardar(falla("b", "Pantalla azul al iniciar", pendiente = true))

        dao.borrarSincronizada("a")
        dao.borrarSincronizada("b")

        assertEquals(listOf("b"), dao.observarTodas().first().map { it.id })
    }

    @Test
    fun borrar_fuera_del_servidor_quita_solo_las_sincronizadas_que_ya_no_existen() = runTest {
        dao.guardar(falla("a", "Fuente de poder dañada", pendiente = false))
        dao.guardar(falla("b", "Pantalla azul al iniciar", pendiente = false))
        dao.guardar(falla("c", "Teclado sin respuesta", pendiente = true))

        dao.borrarSincronizadasFueraDe(listOf("b"))

        assertEquals(listOf("b", "c"), dao.observarTodas().first().map { it.id })
    }

    private fun falla(id: String, titulo: String, origen: String? = null, pendiente: Boolean = true) =
        FallaDiccionarioEntity(
            id = id, titulo = titulo, sintomas = "No enciende", componente = "Fuente de poder",
            tipo = TipoFalla.HARDWARE, solucion = "Reemplazar la fuente", autorId = "u-luis",
            autorNombre = "Luis Ramírez", incidenciaOrigenId = origen, pendienteSincronizar = pendiente,
            actualizadoEn = ahora
        )
}
