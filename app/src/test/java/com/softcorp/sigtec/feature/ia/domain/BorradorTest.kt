package com.softcorp.sigtec.feature.ia.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-14 (RF-1419, RF-1423): un borrador sirve si trae al menos el diagnóstico y el trabajo realizado. */
class BorradorTest {

    private val completo = BorradorInforme(
        diagnostico = "Falla en la fuente de poder por variación de voltaje.",
        trabajoRealizado = "Se reemplazó la fuente por una ATX de 500 W.",
        piezasCambiadas = "Fuente ATX de 500 W",
        observaciones = "Se recomienda un estabilizador."
    )

    @Test
    fun `lee los cuatro campos`() {
        val texto = """
            {"diagnostico": "Falla en la fuente de poder por variación de voltaje.",
             "trabajoRealizado": "Se reemplazó la fuente por una ATX de 500 W.",
             "piezasCambiadas": "Fuente ATX de 500 W",
             "observaciones": "Se recomienda un estabilizador."}
        """.trimIndent()
        assertEquals(completo, leerBorrador(texto))
    }

    @Test
    fun `sin piezas ni observaciones tambien sirve`() {
        val r = leerBorrador("""{"diagnostico": " Cable de red suelto ", "trabajoRealizado": "Se conectó el cable."}""")
        assertEquals(BorradorInforme("Cable de red suelto", "Se conectó el cable.", "", ""), r)
    }

    @Test
    fun `sin diagnostico o sin trabajo realizado no sirve`() {
        assertNull(leerBorrador("""{"diagnostico": "", "trabajoRealizado": "Se conectó el cable."}"""))
        assertNull(leerBorrador("""{"diagnostico": "Cable suelto", "trabajoRealizado": "   "}"""))
        assertNull(leerBorrador("""{"diagnostico": "Cable suelto"}"""))
    }

    @Test
    fun `un campo que no es texto queda vacio`() {
        val r = leerBorrador("""{"diagnostico": "Cable suelto", "trabajoRealizado": "Se conectó.", "piezasCambiadas": null}""")
        assertEquals("", r?.piezasCambiadas)
    }

    @Test
    fun `cada campo se recorta al maximo`() {
        val largo = "a".repeat(MAX_CARACTERES_CAMPO + 50)
        val r = leerBorrador("""{"diagnostico": "$largo", "trabajoRealizado": "Se conectó."}""")
        assertEquals(MAX_CARACTERES_CAMPO, r?.diagnostico?.length)
    }

    @Test
    fun `acepta el bloque json que a veces agrega el modelo`() {
        val r = leerBorrador("```json\n{\"diagnostico\": \"Cable suelto\", \"trabajoRealizado\": \"Se conectó.\"}\n```")
        assertEquals("Cable suelto", r?.diagnostico)
    }

    @Test
    fun `texto suelto no sirve`() {
        assertNull(leerBorrador("Se diagnosticó una falla en la fuente de poder."))
        assertNull(leerBorrador(""))
    }

    @Test
    fun `el contenido guardado se vuelve a leer igual`() {
        assertEquals(completo, leerBorrador(completo.comoContenido()))
    }
}
