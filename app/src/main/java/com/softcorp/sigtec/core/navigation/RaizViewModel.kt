package com.softcorp.sigtec.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.usecase.CerrarSesion
import com.softcorp.sigtec.core.domain.usecase.IniciarSesion
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RaizViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActual,
    private val iniciarSesion: IniciarSesion,
    private val cerrarSesion: CerrarSesion
) : ViewModel() {

    val usuarioActual = observarUsuarioActual()

    // TEMPORAL: la HU-01 mueve el ingreso a un LoginViewModel con correo y contraseña reales.
    fun entrarDemo(correo: String) {
        viewModelScope.launch { iniciarSesion(correo, contrasena = "") }
    }

    fun salir() {
        viewModelScope.launch { cerrarSesion() }
    }
}