package com.softcorp.sigtec.feature.captura.domain

/** RF-905: cuántas imágenes seguidas deben leer el mismo código para darlo por reconocido. */
const val LECTURAS_IGUALES = 3

/**
 * Las lecturas seguidas del mismo código mientras la cámara analiza. Una imagen sin código o con
 * otro código corta la racha, para no mostrar lecturas sueltas equivocadas (RF-905).
 */
data class RachaLectura(val codigo: String? = null, val veces: Int = 0) {

    val estable: Boolean get() = codigo != null && veces >= LECTURAS_IGUALES

    fun siguiente(lectura: String?): RachaLectura = when (lectura) {
        null -> RachaLectura()
        codigo -> copy(veces = veces + 1)
        else -> RachaLectura(lectura, 1)
    }
}
