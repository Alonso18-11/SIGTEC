package com.softcorp.sigtec.feature.captura.domain

import kotlinx.coroutines.flow.StateFlow

/** HU-10: la linterna del celular, con el flash de la cámara trasera (D-8). */
interface Linterna {

    /** Falso si el celular no tiene flash: entonces no se muestra el botón (RF-1024). */
    val tieneFlash: Boolean

    /** El estado real del flash, también si se apaga desde fuera de la app (RF-1029). */
    val encendida: StateFlow<Boolean>

    /** Enciende el flash; devuelve `false` si no se pudo, por ejemplo con la cámara en uso (RF-1028). */
    fun encender(): Boolean

    /** Apaga el flash; si ya estaba apagado no hace nada (RF-1026, RF-1027). */
    fun apagar()
}
