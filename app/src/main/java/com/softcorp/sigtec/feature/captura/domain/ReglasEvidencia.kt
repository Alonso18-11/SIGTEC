package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.EstadoIncidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.Usuario

/** RF-929 (D-2): máximo de fotos de una incidencia en este celular. */
const val MAX_FOTOS = 10

/**
 * HU-09: por qué [usuario] no puede agregar ni eliminar fotos de [incidencia], o `null` si puede.
 * Solo el técnico asignado, mientras la incidencia siga pendiente (RF-923, RF-926, RF-937, D-1), y sin pasar de
 * [MAX_FOTOS] (RF-929). Al eliminar se pasa [cantidad] `null`, porque el máximo no aplica. La comparten los casos
 * de uso y la pantalla, que con ella deshabilita el disparador (RF-931).
 */
fun bloqueoParaFotos(usuario: Usuario?, incidencia: Incidencia?, cantidad: Int?): ErrorDominio? = when {
    usuario == null || !usuario.perfil.puede(Permiso.ATENDER_INCIDENCIA) -> ErrorDominio.SinPermiso
    incidencia == null || incidencia.tecnicoAsignadoId != usuario.id -> ErrorDominio.SinPermiso
    incidencia.estado == EstadoIncidencia.SOLUCIONADO -> ErrorDominio.IncidenciaCerrada
    cantidad != null && cantidad >= MAX_FOTOS -> ErrorDominio.LimiteFotosAlcanzado
    else -> null
}
