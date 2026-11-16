package com.softcorp.sigtec.feature.indicadores.domain

import com.softcorp.sigtec.core.di.DefaultDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import javax.inject.Inject

/**
 * HU-16 (RF-1602, RF-1604, RF-1622): el historial de un equipo para cualquier perfil con `CONSULTAR_HISTORIAL`.
 * Normaliza el código y recalcula cuando cambian los datos de Room o el [reloj] (para que los 90 días se corran
 * solos), fuera del hilo principal.
 */
class ObservarHistorialEquipo @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: HistorialRepository,
    @DefaultDispatcher private val calculo: CoroutineDispatcher
) {
    operator fun invoke(codigoEquipo: String, reloj: Flow<Instant>): Resultado<Flow<HistorialEquipo>> {
        when (val r = verificarPermiso(Permiso.CONSULTAR_HISTORIAL)) {
            is Resultado.Error -> return r
            is Resultado.Exito -> Unit
        }
        val codigo = normalizarCodigoEquipo(codigoEquipo)
        if (codigo.isEmpty()) return Resultado.Error(ErrorDominio.CampoObligatorio)
        return Resultado.Exito(
            combine(repo.observarDatos(codigo), reloj) { d, ahora ->
                calcularHistorial(codigo, d.equipo, d.incidencias, d.cierres, d.informes, ahora)
            }.flowOn(calculo)
        )
    }
}
