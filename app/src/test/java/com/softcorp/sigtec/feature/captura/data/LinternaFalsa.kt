package com.softcorp.sigtec.feature.captura.data

import com.softcorp.sigtec.feature.captura.domain.Linterna
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** HU-10: linterna en memoria. Con [fallar], `encender()` no enciende, como con la cámara en uso (RF-1028). */
class LinternaFalsa(override val tieneFlash: Boolean = true) : Linterna {

    private val _encendida = MutableStateFlow(false)
    override val encendida: StateFlow<Boolean> = _encendida

    var fallar: Boolean = false

    /** Cuántas veces se pidió apagar, para comprobar que se apaga al salir. */
    var apagados: Int = 0
        private set

    override fun encender(): Boolean {
        if (fallar || !tieneFlash) return false
        _encendida.value = true
        return true
    }

    override fun apagar() {
        apagados++
        _encendida.value = false
    }

    /** Simula que alguien la apaga desde los ajustes rápidos de Android (RF-1029). */
    fun apagarDesdeFuera() {
        _encendida.value = false
    }
}
