package com.softcorp.sigtec.feature.ia.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** HU-14 (RF-1415, P-10): si hay conexión para pedirle algo a la IA. Así el informe no depende de `core/sync`. */
class ObservarIADisponible @Inject constructor(private val asistente: AsistenteIA) {
    operator fun invoke(): Flow<Boolean> = asistente.disponible
}
