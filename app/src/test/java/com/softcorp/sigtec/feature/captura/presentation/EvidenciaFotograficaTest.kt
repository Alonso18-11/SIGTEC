package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** HU-09 (RF-924, RF-925): rótulos de las miniaturas y cantidad de fotos. */
class EvidenciaFotograficaTest {

    private fun foto(id: String, tipo: TipoEvidencia) =
        Evidencia(id, "i124", tipo, "/evidencias/i124/$id.jpg", Instant.parse("2026-10-18T15:00:00Z"))

    @Test
    fun `cada tipo se numera por separado en el orden en que se tomaron`() {
        val fotos = listOf(
            foto("a", TipoEvidencia.FALLA),
            foto("b", TipoEvidencia.PIEZA_CAMBIADA),
            foto("c", TipoEvidencia.FALLA),
            foto("d", TipoEvidencia.PIEZA_CAMBIADA)
        )

        assertEquals(listOf("Falla 1", "Pieza 1", "Falla 2", "Pieza 2"), rotulosFotos(fotos))
    }

    @Test
    fun `sin fotos no hay rotulos`() {
        assertEquals(emptyList<String>(), rotulosFotos(emptyList()))
    }

    @Test
    fun `la cantidad usa singular con una sola foto`() {
        assertEquals("0 fotos", cantidadDeFotos(0))
        assertEquals("1 foto", cantidadDeFotos(1))
        assertEquals("2 fotos", cantidadDeFotos(2))
    }

    @Test
    fun `la pastilla dice como se guardara la foto`() {
        assertEquals("foto de la falla", nombreDelTipo(TipoEvidencia.FALLA))
        assertEquals("foto de la pieza cambiada", nombreDelTipo(TipoEvidencia.PIEZA_CAMBIADA))
    }
}
