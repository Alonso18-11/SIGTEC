package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * HU-14 (RF-1401 a RF-1407, plan 4.1): pide a Gemini el tipo de falla y la prioridad de una incidencia sin clasificar
 * y guarda la sugerencia sin decidir. Devuelve null cuando no corresponde pedirla (ya clasificada, cerrada o ya pedida
 * en este celular). Un error de la IA se devuelve igual: quien lo llama decide no mostrar nada (RF-1406).
 */
class SugerirClasificacion @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val asistente: AsistenteIA,
    private val repo: SugerenciaIARepository
) {
    suspend operator fun invoke(incidencia: Incidencia, ahora: Instant): Resultado<SugerenciaIA?> {
        // RF-1402: sin el permiso no se llama a Gemini
        when (val r = verificarPermiso(Permiso.ASIGNAR_INCIDENCIA)) {
            is Resultado.Exito -> Unit
            is Resultado.Error -> return r
        }
        // RF-1407: ya clasificada, o cerrada, no se pide
        if (incidencia.marca == MarcaSeguimiento.CERRADA || incidencia.tipoFalla != null || incidencia.prioridad != null) {
            return Resultado.Exito(null)
        }
        // RF-1405: una consulta por incidencia y celular
        when (val r = repo.contar(incidencia.id, TipoSugerencia.CLASIFICACION)) {
            is Resultado.Exito -> if (r.valor > 0) return Resultado.Exito(null)
            is Resultado.Error -> return r
        }
        val texto = when (val r = asistente.clasificar(incidencia.descripcion)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val clasificacion = leerClasificacion(texto) ?: return Resultado.Error(ErrorDominio.IANoDisponible)
        val sugerencia = SugerenciaIA(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoSugerencia.CLASIFICACION,
            contenido = clasificacion.comoContenido(),
            modelo = asistente.modelo,
            generadaEn = ahora
        )
        return when (val r = repo.guardar(sugerencia)) {
            is Resultado.Exito -> Resultado.Exito(sugerencia)    // RF-1404
            is Resultado.Error -> r
        }
    }
}
