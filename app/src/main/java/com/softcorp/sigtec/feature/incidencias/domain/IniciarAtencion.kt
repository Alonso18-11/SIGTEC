package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * HU-08: el técnico asignado marca que empezó a atender su incidencia (RF-806 a RF-811). Solo
 * escribe en Room; subirla a Firestore es trabajo exclusivo de core/sync.
 */
class IniciarAtencion @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: IncidenciaRepository
) {
    suspend operator fun invoke(incidenciaId: String, ahora: Instant): Resultado<Incidencia> {
        // RF-807: en el dominio, aunque el detalle solo muestre el botón a quien corresponde
        val tecnico = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        // D-6 del plan: lectura fresca, no el estado que ya tiene la pantalla
        val incidencia = repo.porId(incidenciaId)
            ?: return Resultado.Error(ErrorDominio.Desconocido("incidencia no encontrada"))
        bloqueoParaIniciar(incidencia, tecnico.id)?.let { return Resultado.Error(it) }  // RF-807, RF-808

        val actualizada = incidencia.copy(marca = MarcaSeguimiento.EN_ATENCION)
        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoMovimiento.ATENCION_INICIADA,
            autorNombre = tecnico.nombre,
            fecha = ahora
        )
        return when (val r = repo.iniciarAtencion(actualizada, movimiento, ahora)) {
            is Resultado.Exito -> Resultado.Exito(actualizada)
            is Resultado.Error -> r                                                     // RF-810
        }
    }
}

/**
 * RF-807: solo el técnico asignado puede iniciar la atención.
 * RF-808: la marca debe seguir siendo Asignada cuando se intenta guardar.
 */
fun bloqueoParaIniciar(incidencia: Incidencia, tecnicoId: String): ErrorDominio? = when {
    incidencia.tecnicoAsignadoId != tecnicoId -> ErrorDominio.SinPermiso
    incidencia.marca != MarcaSeguimiento.ASIGNADA -> ErrorDominio.MarcaCambiada
    else -> null
}
