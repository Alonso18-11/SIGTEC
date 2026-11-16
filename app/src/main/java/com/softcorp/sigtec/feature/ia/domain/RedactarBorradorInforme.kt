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
 * HU-14 (RF-1416, RF-1417, RF-1419, RF-1422, RF-1423, plan 4.2): el técnico asignado pide a Gemini el borrador del
 * informe a partir de sus notas. Se guarda sin decidir; el informe no cambia hasta que él lo use (RF-1428).
 */
class RedactarBorradorInforme @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val asistente: AsistenteIA,
    private val repo: SugerenciaIARepository
) {
    suspend operator fun invoke(incidencia: Incidencia, entrada: EntradaBorrador, ahora: Instant): Resultado<SugerenciaIA> {
        val tecnico = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        // RF-1416: solo el técnico asignado, mientras siga pendiente
        if (incidencia.tecnicoAsignadoId != tecnico.id || incidencia.marca == MarcaSeguimiento.CERRADA) {
            return Resultado.Error(ErrorDominio.SinPermiso)
        }
        // RF-1415: sin notas no hay nada que ordenar (la pantalla ya deshabilita el botón)
        if (entrada.notas.isBlank()) return Resultado.Error(ErrorDominio.CampoObligatorio)

        val anterior = when (val r = repo.sinDecidir(incidencia.id, TipoSugerencia.BORRADOR_INFORME)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val texto = when (val r = asistente.redactarBorrador(entrada)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r                                  // RF-1423: el anterior sigue a la vista
        }
        val borrador = leerBorrador(texto) ?: return Resultado.Error(ErrorDominio.IANoDisponible)

        // RF-1422: el anterior sin decidir queda como no aceptado recién cuando hay uno nuevo
        if (anterior != null) {
            when (val r = repo.decidir(anterior.id, aceptada = false, decididaPorId = tecnico.id, ahora = ahora)) {
                is Resultado.Exito -> Unit
                is Resultado.Error -> return r
            }
        }
        val sugerencia = SugerenciaIA(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoSugerencia.BORRADOR_INFORME,
            contenido = borrador.comoContenido(),
            modelo = asistente.modelo,
            generadaEn = ahora
        )
        return when (val r = repo.guardar(sugerencia)) {
            is Resultado.Exito -> Resultado.Exito(sugerencia)               // RF-1419
            is Resultado.Error -> r
        }
    }
}
