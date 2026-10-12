package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.entity.TecnicoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-06 (RF-619): cómo un documento de /usuarios con perfil técnico llega a la tabla tecnicos. */
class MapeoTecnicoTest {

    private val luis = mapOf<String, Any?>(
        "nombre" to "Luis Ramírez", "perfil" to "TECNICO",
        "limiteAtencion" to 5L, "incidenciasActivas" to 3L
    )

    @Test
    fun `un tecnico con todos sus datos se guarda con los enteros convertidos`() {
        assertEquals(TecnicoEntity("u1", "Luis Ramírez", 5, 3), tecnicoDesdeDatos("u1", luis))
    }

    @Test
    fun `sin limite de atencion se descarta`() {
        assertNull(tecnicoDesdeDatos("u1", luis - "limiteAtencion"))
    }

    @Test
    fun `sin nombre se descarta`() {
        assertNull(tecnicoDesdeDatos("u1", luis - "nombre"))
    }

    @Test
    fun `sin incidencias activas registradas empieza en cero`() {
        assertEquals(0, tecnicoDesdeDatos("u1", luis - "incidenciasActivas")?.incidenciasActivas)
    }
}
