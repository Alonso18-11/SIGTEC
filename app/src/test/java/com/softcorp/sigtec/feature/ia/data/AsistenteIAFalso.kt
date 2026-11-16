package com.softcorp.sigtec.feature.ia.data

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.ia.domain.AsistenteIA
import com.softcorp.sigtec.feature.ia.domain.EntradaBorrador
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de [AsistenteIA]: responde lo que se le fije y anota lo que le pidieron. Nunca llama a Gemini. */
class AsistenteIAFalso(
    var respuestaClasificacion: Resultado<String> = Resultado.Exito("""{"tipo": "RED", "prioridad": "MEDIA"}"""),
    var respuestaBorrador: Resultado<String> = Resultado.Exito(
        """{"diagnostico": "Fuente dañada por voltaje", "trabajoRealizado": "Se cambió la fuente", """ +
            """"piezasCambiadas": "Fuente ATX de 500 W", "observaciones": ""}"""
    )
) : AsistenteIA {

    override val modelo = "modelo-de-prueba"

    override val disponible = MutableStateFlow(true)

    val descripcionesClasificadas = mutableListOf<String>()
    val entradasDeBorrador = mutableListOf<EntradaBorrador>()

    override suspend fun clasificar(descripcion: String): Resultado<String> {
        descripcionesClasificadas += descripcion
        return respuestaClasificacion
    }

    override suspend fun redactarBorrador(entrada: EntradaBorrador): Resultado<String> {
        entradasDeBorrador += entrada
        return respuestaBorrador
    }
}
