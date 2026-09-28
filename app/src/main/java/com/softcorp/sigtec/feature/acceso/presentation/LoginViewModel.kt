package com.softcorp.sigtec.feature.acceso.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.usecase.IniciarSesion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: ErrorDominio? = null
) {
    val puedeIngresar: Boolean
        get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesion
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun alCambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }

    fun alCambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

    fun alternarVisibilidad() = _estado.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }

    fun ingresar() {
        val actual = _estado.value
        if (!actual.puedeIngresar) return
        _estado.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            val resultado = iniciarSesion(actual.correo, actual.contrasena)
            // Si fue exitoso no se navega desde aquí: SigtecRaiz reacciona a la sesión.
            _estado.update {
                it.copy(cargando = false, error = (resultado as? Resultado.Error)?.causa)
            }
        }
    }
}