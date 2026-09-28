package com.softcorp.sigtec.feature.inicio.presentation

import androidx.lifecycle.ViewModel
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.usecase.ObservarUsuarioActual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActual
) : ViewModel() {
    val usuario: StateFlow<Usuario?> = observarUsuarioActual()
}