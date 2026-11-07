package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.EvidenciaDao
import com.softcorp.sigtec.core.database.entity.EvidenciaEntity
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-09: las fotos de una incidencia salen en orden de captura (RF-932) y se cuentan todas (RF-943). */
@RunWith(AndroidJUnit4::class)
class EvidenciaDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: EvidenciaDao

    private val ahora = Instant.parse("2026-11-07T10:00:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.evidenciaDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun las_fotos_de_una_incidencia_salen_en_orden_de_captura() = runTest {
        dao.guardar(evidencia("b", "inc-1", minutos = 5))
        dao.guardar(evidencia("a", "inc-1", minutos = 1))
        dao.guardar(evidencia("c", "inc-2", minutos = 0))

        assertEquals(listOf("a", "b"), dao.observarPorIncidencia("inc-1").first().map { it.id })
    }

    @Test
    fun contar_suma_las_fotos_de_todas_las_incidencias() = runTest {
        assertEquals(0, dao.contar().first())
        dao.guardar(evidencia("a", "inc-1"))
        dao.guardar(evidencia("b", "inc-2"))
        assertEquals(2, dao.contar().first())

        dao.eliminar(evidencia("a", "inc-1"))

        assertEquals(1, dao.contar().first())
    }

    private fun evidencia(id: String, incidenciaId: String, minutos: Long = 0) = EvidenciaEntity(
        id = id,
        incidenciaId = incidenciaId,
        tipo = TipoEvidencia.FALLA,
        rutaArchivo = "/evidencias/$incidenciaId/$id.jpg",
        fechaCaptura = ahora.plusSeconds(minutos * 60)
    )
}
