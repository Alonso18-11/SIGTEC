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

/** HU-03: registra una incidencia a nombre del usuario de la sesión. */
class RegistrarIncidencia @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: IncidenciaRepository
) {
    suspend operator fun invoke(
        codigoEquipo: String, descripcion: String, responsable: String
    ): Resultado<Incidencia> {
        val usuario = when (val r = verificarPermiso(Permiso.REGISTRAR_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val codigo = codigoEquipo.trim().uppercase()
        val texto = descripcion.trim()
        val quien = responsable.trim()
        // La pantalla ya no deja guardar con campos vacíos; esto cubre cualquier otro llamador
        if (codigo.isEmpty() || texto.isEmpty() || quien.isEmpty()) {
            return Resultado.Error(ErrorDominio.CampoObligatorio)
        }

        val incidencia = Incidencia(
            id = UUID.randomUUID().toString(),
            numero = null,                          // lo asigna la sincronización
            codigoEquipo = codigo,
            descripcion = texto,
            usuarioResponsable = quien,
            fechaRegistro = Instant.now(),          // automática, no editable
            registradoPorId = usuario.id,
            registradoPorNombre = usuario.nombre,
            marca = MarcaSeguimiento.SIN_ASIGNAR    // nace Pendiente
        )
        // Primer punto de la línea de tiempo del detalle
        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            incidenciaId = incidencia.id,
            tipo = TipoMovimiento.REGISTRADA,
            autorNombre = usuario.nombre,
            fecha = incidencia.fechaRegistro
        )
        return when (val r = repo.registrar(incidencia, movimiento)) {
            is Resultado.Exito -> Resultado.Exito(incidencia)
            is Resultado.Error -> r
        }
    }
}