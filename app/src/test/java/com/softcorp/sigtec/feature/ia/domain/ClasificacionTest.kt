package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-14 (RF-1401, RF-1406): solo se muestra una clasificación con un tipo y una prioridad permitidos. */
class ClasificacionTest {

    @Test
    fun `lee el tipo y la prioridad`() {
        assertEquals(
            ClasificacionSugerida(TipoFalla.RED, Prioridad.MEDIA),
            leerClasificacion("""{"tipo": "RED", "prioridad": "MEDIA"}""")
        )
    }

    @Test
    fun `acepta minusculas, espacios y campos de mas`() {
        assertEquals(
            ClasificacionSugerida(TipoFalla.HARDWARE, Prioridad.ALTA),
            leerClasificacion("""  {"tipo": " hardware ", "prioridad": "Alta", "motivo": "no enciende"}  """)
        )
    }

    @Test
    fun `acepta el bloque json que a veces agrega el modelo`() {
        assertEquals(
            ClasificacionSugerida(TipoFalla.SOFTWARE, Prioridad.BAJA),
            leerClasificacion("```json\n{\"tipo\": \"SOFTWARE\", \"prioridad\": \"BAJA\"}\n```")
        )
    }

    @Test
    fun `un tipo fuera de los permitidos no sirve`() {
        assertNull(leerClasificacion("""{"tipo": "ELECTRICO", "prioridad": "ALTA"}"""))
    }

    @Test
    fun `una prioridad fuera de las permitidas no sirve`() {
        assertNull(leerClasificacion("""{"tipo": "RED", "prioridad": "URGENTE"}"""))
    }

    @Test
    fun `si falta un campo o no es texto, no sirve`() {
        assertNull(leerClasificacion("""{"tipo": "RED"}"""))
        assertNull(leerClasificacion("""{"tipo": "RED", "prioridad": 2}"""))
    }

    @Test
    fun `texto suelto o un arreglo no sirven`() {
        assertNull(leerClasificacion("Es una falla de red con prioridad media."))
        assertNull(leerClasificacion("""[{"tipo": "RED", "prioridad": "MEDIA"}]"""))
        assertNull(leerClasificacion(""))
    }

    @Test
    fun `el contenido guardado se vuelve a leer igual`() {
        val sugerida = ClasificacionSugerida(TipoFalla.RED, Prioridad.ALTA)
        assertEquals("""{"tipo":"RED","prioridad":"ALTA"}""", sugerida.comoContenido())
        assertEquals(sugerida, leerClasificacion(sugerida.comoContenido()))
    }
}
