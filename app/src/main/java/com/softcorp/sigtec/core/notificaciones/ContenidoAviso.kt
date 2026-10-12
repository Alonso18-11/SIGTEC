package com.softcorp.sigtec.core.notificaciones

import com.softcorp.sigtec.core.domain.model.Incidencia

/** Lo que muestra una notificación: completa con el celular desbloqueado, solo el título si no (RF-707). */
data class ContenidoAviso(
    val titulo: String,
    val texto: String,
    val textoPublico: String
)

/**
 * HU-07 (RF-702, RF-707): «Nueva incidencia asignada» con «INC-0127 · IMP-RRHH-002 · La impresora…».
 * Una incidencia asignada siempre tiene número (RF-610 de la HU-06); si faltara, se dice «Incidencia».
 */
fun contenidoAvisoAsignacion(incidencia: Incidencia): ContenidoAviso {
    val titulo = "Nueva incidencia asignada"
    val codigo = incidencia.codigoVisible ?: "Incidencia"
    return ContenidoAviso(
        titulo = titulo,
        texto = "$codigo · ${incidencia.codigoEquipo} · ${incidencia.descripcion}",
        textoPublico = titulo
    )
}

/** RF-704: el mismo id para la misma incidencia, así un segundo aviso reemplaza al primero (y la HU-17 lo reutiliza). */
fun idNotificacion(incidenciaId: String): Int = incidenciaId.hashCode()
