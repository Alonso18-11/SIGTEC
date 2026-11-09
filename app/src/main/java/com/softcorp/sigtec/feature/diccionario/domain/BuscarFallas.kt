package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.TipoFalla
import java.text.Normalizer

private val MARCAS = Regex("\\p{Mn}+")
private val NO_ALFANUMERICO = Regex("[^a-z0-9ñ]+")
private val ESPACIOS = Regex("\\s+")

/**
 * Minúsculas, sin tildes ni signos, con un solo espacio entre palabras (RF-1304, RF-1325). La «ñ» se conserva:
 * «año» y «ano» no son lo mismo.
 */
fun normalizar(texto: String): String {
    val minusculas = texto.lowercase().replace('ñ', '\u0000')
    val sinTildes = MARCAS.replace(Normalizer.normalize(minusculas, Normalizer.Form.NFD), "").replace('\u0000', 'ñ')
    return NO_ALFANUMERICO.replace(sinTildes, " ").trim()
}

/**
 * RF-1304, RF-1305: filtra por tipo y por todas las palabras escritas, cada una en cualquiera de los campos (título,
 * síntomas, componente o solución). Sin texto ni tipo devuelve la lista como llegó.
 */
fun filtrarFallas(fallas: List<FallaDiccionario>, texto: String, tipo: TipoFalla?): List<FallaDiccionario> {
    val palabras = normalizar(texto).split(ESPACIOS).filter { it.isNotEmpty() }
    return fallas.filter { falla ->
        (tipo == null || falla.tipo == tipo) && (palabras.isEmpty() || contieneTodas(falla, palabras))
    }
}

private fun contieneTodas(falla: FallaDiccionario, palabras: List<String>): Boolean {
    val campos = normalizar("${falla.titulo} ${falla.sintomas} ${falla.componente} ${falla.solucion}")
    return palabras.all { it in campos }
}
