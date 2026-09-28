package com.softcorp.sigtec.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloqueoApp @Inject constructor() {
    private val _desbloqueada = MutableStateFlow(false)
    val desbloqueada: StateFlow<Boolean> = _desbloqueada.asStateFlow()

    fun desbloquear() { _desbloqueada.value = true }
    fun bloquear() { _desbloqueada.value = false }
}