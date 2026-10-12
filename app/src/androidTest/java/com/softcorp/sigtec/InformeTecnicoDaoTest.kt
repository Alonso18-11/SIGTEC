package com.softcorp.sigtec

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.InformeTecnicoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** HU-08 (RF-825): qué informes suben y cómo se marcan cuando su incidencia (el padre) se rechaza. */
@RunWith(AndroidJUnit4::class)
class InformeTecnicoDaoTest {

    private lateinit var db: SigtecDatabase
    private lateinit var incidencias: IncidenciaDao
    private lateinit var dao: InformeTecnicoDao

    private val ahora = Instant.parse("2026-10-12T10:00:00Z")

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SigtecDatabase::class.java
        ).build()
        incidencias = db.incidenciaDao()
        dao = db.informeTecnicoDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun solo_suben_los_informes_cuya_incidencia_ya_esta_en_el_servidor() = runTest {
        incidencias.guardar(incidencia("subida", pendiente = false))
        incidencias.guardar(incidencia("pendiente", pendiente = true))
        incidencias.guardar(incidencia("rechazada", pendiente = false, atencion = true))
        dao.guardar(informe("inf-subida", "subida"))                 // padre sincronizado -> sale
        dao.guardar(informe("inf-pendiente", "pendiente"))            // padre pendiente -> no sale
        dao.guardar(informe("inf-rechazada", "rechazada"))            // padre rechazado -> no sale
        dao.guardar(informe("inf-sin-incidencia", "no-esta-en-el-celular"))

        // RF-825
        val listos = dao.listosParaSubir().map { it.id }.sorted()

        assertEquals(listOf("inf-sin-incidencia", "inf-subida"), listos)
    }

    @Test
    fun una_incidencia_rechazada_bloquea_solo_su_informe_pendiente() = runTest {
        dao.guardar(informe("inf1", "rechazada"))
        dao.guardar(informe("inf2", "otra"))
        dao.guardar(informe("inf3", "ya-sincronizada", pendiente = false))

        // RF-825
        dao.marcarNecesitaAtencionDe("rechazada")
        dao.marcarNecesitaAtencionDe("ya-sincronizada")

        val inf1 = dao.porId("inf1")!!
        assertEquals(false, inf1.pendienteSincronizar)
        assertEquals(true, inf1.necesitaAtencion)
        assertEquals(true, dao.porId("inf2")!!.pendienteSincronizar)
        assertEquals(false, dao.porId("inf2")!!.necesitaAtencion)
        // ya estaba en el servidor: la condición "pendienteSincronizar = 1" no lo alcanza
        assertEquals(false, dao.porId("inf3")!!.necesitaAtencion)
    }

    @Test
    fun borrar_rechazados_quita_solo_los_que_nunca_llegaron_al_servidor() = runTest {
        dao.guardar(informe("inf-rechazado", "rechazada", pendiente = false).copy(necesitaAtencion = true))
        dao.guardar(informe("inf-normal", "normal", pendiente = false))

        // RF-825
        dao.borrarRechazados()

        assertNull(dao.porId("inf-rechazado"))
        assertEquals("inf-normal", dao.porId("inf-normal")!!.id)
    }

    private fun incidencia(
        id: String,
        pendiente: Boolean,
        atencion: Boolean = false,
        numero: Int? = 1
    ) = IncidenciaEntity(
        id = id, numero = numero, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
        usuarioResponsable = "María Quispe", fechaRegistro = ahora, registradoPorId = "t1",
        registradoPorNombre = "Luis Ramírez", marca = MarcaSeguimiento.ASIGNADA, tipoFalla = null,
        prioridad = null, tecnicoAsignadoId = "t1", tecnicoAsignadoNombre = "Luis Ramírez",
        critica = false, pendienteSincronizar = pendiente, actualizadoEn = ahora,
        necesitaAtencion = atencion
    )

    private fun informe(id: String, incidenciaId: String, pendiente: Boolean = true) = InformeTecnicoEntity(
        id = id, incidenciaId = incidenciaId, diagnostico = "Fuente dañada",
        trabajoRealizado = "Se cambió la fuente", piezasCambiadas = null, observaciones = null,
        fechaCierre = ahora, confirmadoEn = ahora, pendienteSincronizar = pendiente
    )
}
