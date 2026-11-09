package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import kotlin.math.ln

private const val LARGO_MINIMO = 3
private const val PESO_TITULO = 3.0
private const val PESO_COMPONENTE = 2.0
private const val PESO_SINTOMAS = 1.0
private const val MAXIMO_SUGERENCIAS = 3

// Palabras que no dicen nada de la falla (RF-1325). Se comparan ya normalizadas, sin tildes.
private val PALABRAS_VACIAS = setOf(
    "los", "las", "del", "una", "uno", "unos", "unas", "que", "con", "sin", "por", "para", "como", "pero",
    "mas", "muy", "ya", "esta", "este", "esto", "estos", "estas", "ese", "esa", "eso", "aqui", "alli", "hay",
    "tiene", "tienen", "tenia", "ser", "son", "era", "fue", "han", "hace", "cuando", "desde", "hasta", "sobre",
    "entre", "todo", "toda", "todos", "todas", "otro", "otra", "algo", "nada", "solo", "tambien", "porque",
    "cada", "vez", "veces", "dia", "dias", "sus", "nos", "les", "mismo", "misma", "puede", "pide", "sale",
    "equipo", "usuario", "problema", "funciona", "despues", "antes", "luego", "bien", "mal"
)

/**
 * RF-1325: las palabras con las que se compara un texto. Normaliza, descarta las cortas y las vacías, y deja la raíz de
 * cada una para que singular y plural coincidan.
 */
internal fun palabrasClave(texto: String): Set<String> =
    normalizar(texto).split(' ')
        .filter { it.length >= LARGO_MINIMO && it !in PALABRAS_VACIAS }
        .map(::raiz)
        .filter { it.length >= LARGO_MINIMO }
        .toSet()

/**
 * Raíz simple para que singular y plural den lo mismo: «luces» y «luz» → «luz»; «fuentes» y «fuente» → «fuent»;
 * «errores» y «error» → «error»; «redes» y «red» → «red». No se muestra: solo sirve para comparar.
 */
internal fun raiz(palabra: String): String {
    if (palabra.length > 4 && palabra.endsWith("ces")) return palabra.dropLast(3) + "z"
    var r = palabra
    if (r.length > LARGO_MINIMO && r.endsWith("s")) r = r.dropLast(1)
    if (r.length > LARGO_MINIMO && r.endsWith("e") && r[r.length - 2] !in VOCALES) r = r.dropLast(1)
    return r
}

private const val VOCALES = "aeiou"

/** Las palabras de una falla por campo, calculadas una sola vez por falla. */
private class PalabrasFalla(val falla: FallaDiccionario) {
    val titulo = palabrasClave(falla.titulo)
    val componente = palabrasClave(falla.componente)
    val sintomas = palabrasClave(falla.sintomas)
    val todas = titulo + componente + sintomas
}

/**
 * HU-13 (RF-1324 a RF-1326, D-1): las fallas del diccionario más parecidas a la descripción de una incidencia, de la
 * más parecida a la menos, hasta [maximo]. Cada palabra en común suma su peso por campo (título 3, componente 2,
 * síntomas 1) por lo poco común que es en el diccionario. Solo entran las que tienen al menos una palabra en común.
 */
fun fallasParecidas(
    descripcion: String,
    fallas: List<FallaDiccionario>,
    maximo: Int = MAXIMO_SUGERENCIAS
): List<FallaDiccionario> {
    val buscadas = palabrasClave(descripcion)
    if (buscadas.isEmpty() || fallas.isEmpty()) return emptyList()

    val porFalla = fallas.map(::PalabrasFalla)
    val total = porFalla.size.toDouble()
    // En cuántas fallas aparece cada palabra buscada (para la rareza)
    val apariciones = buscadas.associateWith { palabra -> porFalla.count { palabra in it.todas } }

    return porFalla
        .map { pf ->
            val puntaje = buscadas.sumOf { palabra ->
                val veces = apariciones.getValue(palabra)
                if (veces == 0) 0.0 else {
                    val peso = (if (palabra in pf.titulo) PESO_TITULO else 0.0) +
                        (if (palabra in pf.componente) PESO_COMPONENTE else 0.0) +
                        (if (palabra in pf.sintomas) PESO_SINTOMAS else 0.0)
                    peso * ln(1 + total / veces)
                }
            }
            pf.falla to puntaje
        }
        .filter { (_, puntaje) -> puntaje > 0.0 }
        .sortedWith(compareByDescending<Pair<FallaDiccionario, Double>> { it.second }.thenBy { it.first.titulo })
        .take(maximo)
        .map { it.first }
}
