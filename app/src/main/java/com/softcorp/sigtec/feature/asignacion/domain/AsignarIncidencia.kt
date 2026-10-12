package com.softcorp.sigtec.feature.asignacion.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * HU-06: el jefe asigna o reasigna una incidencia pendiente a un técnico con cupo (RF-05, RF-06 del
 * producto). Solo escribe en Room; subirla y mover la carga en el servidor es de core/sync (RF-620).
 */
class AsignarIncidencia @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: AsignacionRepository
) {
    suspend operator fun invoke(incidenciaId: String, tecnicoId: String, ahora: Instant): Resultado<Incidencia> {
        // RF-608: en el dominio, aunque el detalle solo muestre el botón al jefe
        val jefe = when (val r = verificarPermiso(Permiso.ASIGNAR_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        // D-5 del plan: solo pasa si los datos cambian entre la pantalla y el toque
        val incidencia = repo.incidencia(incidenciaId)
            ?: return Resultado.Error(ErrorDominio.Desconocido("incidencia no encontrada"))
        bloqueoParaAsignar(incidencia)?.let { return Resultado.Error(it) }      // RF-609, RF-610
        val tecnico = repo.tecnico(tecnicoId)
            ?: return Resultado.Error(ErrorDominio.Desconocido("técnico no encontrado"))
        if (incidencia.tecnicoAsignadoId == tecnico.id) {
            return Resultado.Error(ErrorDominio.YaAsignada(tecnico.nombre))     // RF-611
        }
        // RF-612: cuenta también lo asignado en este celular que aún no subió (RF-602)
        if (!cargaEfectiva(tecnico, repo.asignacionesSinSubir(tecnico.id)).disponible) {
            return Resultado.Error(ErrorDominio.LimiteAtencionAlcanzado(tecnico.nombre))
        }

        val actualizada = incidencia.copy(
            marca = marcaAlAsignar(incidencia.marca),                          // RF-614, RF-625
            tecnicoAsignadoId = tecnico.id,
            tecnicoAsignadoNombre = tecnico.nombre
        )
        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = tipoDeAsignacion(incidencia.tecnicoAsignadoId),              // RF-613
            autorNombre = jefe.nombre,
            fecha = ahora,
            detalle = detalleDeAsignacion(incidencia.tecnicoAsignadoNombre, tecnico.nombre, jefe.nombre)
        )
        return when (val r = repo.asignar(actualizada, movimiento, ahora)) {
            is Resultado.Exito -> Resultado.Exito(actualizada)
            is Resultado.Error -> r                                             // RF-616
        }
    }
}
