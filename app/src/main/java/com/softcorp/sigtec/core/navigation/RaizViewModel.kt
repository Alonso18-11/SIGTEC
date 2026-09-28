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
    private val cerrarSesion: CerrarSesion
) : ViewModel() {

    val usuarioActual = observarUsuarioActual()

    fun salir() {
        viewModelScope.launch { cerrarSesion() }
    }
}