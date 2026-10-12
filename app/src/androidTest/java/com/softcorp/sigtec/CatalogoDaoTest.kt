package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.entity.TecnicoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** HU-06 (RF-619): los técnicos bajados se guardan y los que dejan de serlo se borran. */
@RunWith(AndroidJUnit4::class)
class CatalogoDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var dao: CatalogoDao

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        dao = db.catalogoDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun guarda_los_tecnicos_y_borra_los_que_dejan_de_serlo() = runTest {
        dao.guardarTecnicos(listOf(
            TecnicoEntity("u-luis", "Luis Ramírez", limiteAtencion = 5, incidenciasActivas = 3),
            TecnicoEntity("u-ana", "Ana Torres", limiteAtencion = 5, incidenciasActivas = 5)
        ))

        dao.borrarTecnicos(listOf("u-ana"))

        assertEquals(listOf("Luis Ramírez"), dao.observarTecnicos().first().map { it.nombre })
    }

    @Test
    fun guardar_de_nuevo_actualiza_la_carga_sin_duplicar() = runTest {
        dao.guardarTecnicos(listOf(TecnicoEntity("u-luis", "Luis Ramírez", 5, 3)))
        dao.guardarTecnicos(listOf(TecnicoEntity("u-luis", "Luis Ramírez", 5, 4)))

        val tecnicos = dao.observarTecnicos().first()
        assertEquals(1, tecnicos.size)
        assertEquals(4, tecnicos.single().incidenciasActivas)
    }
}
