package com.softcorp.sigtec.feature.atencion.domain

import com.softcorp.sigtec.core.domain.Resultado
import java.time.Instant
import javax.inject.Inject

/** Cuántas se registraron, cuántas rechazó Logística y cuántas quedan para el próximo intento. */
data class ResultadoRegistro(val registradas: Int, val rechazadas: Int, val pendientes: Int)

/**
 * HU-11 (plan 4.3, RF-1117, RF-1119): registra en Logística cada solicitud que todavía no tiene número. No necesita
 * `VerificarPermiso`: corre en segundo plano sobre solicitudes que ya se confirmaron con huella al crearlas.
 */
class RegistrarSolicitudesEnLogistica @Inject constructor(
    private val repo: AtencionRepository,
    private val logistica: LogisticaRepository
) {
    suspend operator fun invoke(ahora: Instant): ResultadoRegistro {
        var registradas = 0
        var rechazadas = 0
        var pendientes = 0
        for (solicitud in repo.solicitudesPorRegistrar()) {
            val incidencia = repo.incidencia(solicitud.incidenciaId)
            val codigo = incidencia?.codigoVisible ?: solicitud.incidenciaId
            val equipo = incidencia?.codigoEquipo.orEmpty()
            when (val r = logistica.registrar(solicitud, codigo, equipo)) {
                is ResultadoLogistica.Exito ->
                    if (repo.marcarRegistrada(solicitud.id, r.valor, ahora).esExito()) registradas++ else pendientes++
                is ResultadoLogistica.Fallo -> when (r.error) {
                    ErrorLogistica.RECHAZADA ->
                        if (repo.marcarRechazada(solicitud.id, ahora).esExito()) rechazadas++ else pendientes++
                    ErrorLogistica.SIN_RESPUESTA -> pendientes++   // se reintenta más tarde
                }
            }
        }
        return ResultadoRegistro(registradas, rechazadas, pendientes)
    }
}

private fun Resultado<*>.esExito() = this is Resultado.Exito
