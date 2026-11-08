package com.softcorp.sigtec.feature.captura.data

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.softcorp.sigtec.feature.captura.domain.Linterna
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-10 (P-5): enciende el flash con `CameraManager.setTorchMode`, sin abrir la cámara ni pedir su permiso.
 * Es la única dueña del flash fuera de las pantallas de cámara; con la cámara abierta se usa CameraX (P-6).
 */
@Singleton
class LinternaAndroid @Inject constructor(
    @ApplicationContext contexto: Context
) : Linterna {

    private val camaras: CameraManager? = contexto.getSystemService(CameraManager::class.java)
    private val idCamara: String? = camaras?.let(::camaraConFlash)

    private val _encendida = MutableStateFlow(false)
    override val encendida: StateFlow<Boolean> = _encendida.asStateFlow()

    override val tieneFlash: Boolean get() = idCamara != null

    // RF-1029: Android avisa también los cambios hechos desde los ajustes rápidos o por otra app
    private val aviso = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == idCamara) _encendida.value = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == idCamara) _encendida.value = false
        }
    }

    init {
        if (idCamara != null) camaras?.registerTorchCallback(aviso, Handler(Looper.getMainLooper()))
    }

    override fun encender(): Boolean = cambiar(true)

    override fun apagar() {
        if (_encendida.value) cambiar(false)
    }

    private fun cambiar(encender: Boolean): Boolean {
        val id = idCamara ?: return false
        return try {
            camaras?.setTorchMode(id, encender)
            _encendida.value = encender
            true
        } catch (e: CameraAccessException) {
            Log.w(TAG, "No se pudo cambiar la linterna", e)   // RF-1028: otra app usa la cámara
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "No se pudo cambiar la linterna", e)
            false
        }
    }

    /** La cámara trasera con flash; si no hay, cualquier otra con flash; si ninguna tiene, `null`. */
    private fun camaraConFlash(camaras: CameraManager): String? = try {
        val conFlash = camaras.cameraIdList.filter { id ->
            camaras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        conFlash.firstOrNull { id ->
            camaras.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        } ?: conFlash.firstOrNull()
    } catch (e: CameraAccessException) {
        Log.w(TAG, "No se pudo consultar las cámaras", e)
        null
    }

    private companion object {
        const val TAG = "Linterna"
    }
}
