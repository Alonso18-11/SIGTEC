package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.ui.mensaje

/**
 * HU-09 (RF-931): el texto de un error al agregar fotos. `IncidenciaCerrada` y `SinPermiso` (cerrada o reasignada
 * mientras la pantalla estaba abierta) tienen su propio aviso: el texto general de `IncidenciaCerrada` habla de
 * asignar (HU-06). Los demás errores usan su texto de siempre.
 */
fun mensajeFotos(error: ErrorDominio): String = when (error) {
    ErrorDominio.IncidenciaCerrada, ErrorDominio.SinPermiso -> "Ya no puedes agregar fotos a esta incidencia."
    else -> error.mensaje()
}
