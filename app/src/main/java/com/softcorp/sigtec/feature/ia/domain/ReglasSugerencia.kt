package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla

/**
 * HU-14 (RF-1413, D-4): la clasificación sugerida cuenta como aceptada solo si el jefe tocó «Usar» y asignó exactamente
 * el tipo y la prioridad sugeridos. Si la descartó, si cambió algún valor después de usarla o si asignó sin usarla,
 * queda como no aceptada, aunque por casualidad haya elegido lo mismo a mano.
 */
fun clasificacionAceptada(
    sugerida: ClasificacionSugerida,
    usada: Boolean,
    descartada: Boolean,
    tipoAsignado: TipoFalla?,
    prioridadAsignada: Prioridad?
): Boolean = usada && !descartada && tipoAsignado == sugerida.tipo && prioridadAsignada == sugerida.prioridad
