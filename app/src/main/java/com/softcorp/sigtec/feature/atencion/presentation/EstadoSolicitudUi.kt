package com.softcorp.sigtec.feature.atencion.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible

/** HU-11 (RF-1120, RF-1122): el texto de cada estado de una solicitud de repuesto. Lo usan el detalle y «Estado de repuestos». */
fun textoEstadoSolicitud(estado: EstadoSolicitudVisible): String = when (estado) {
    EstadoSolicitudVisible.POR_ENVIAR -> "Por enviar a Logística"
    EstadoSolicitudVisible.POR_DESPACHAR -> "Por despachar"
    EstadoSolicitudVisible.EN_CAMINO -> "En camino"
    EstadoSolicitudVisible.ENTREGADA -> "Entregado"
    EstadoSolicitudVisible.RECHAZADA -> "Rechazada"
}

/**
 * El color de la etiqueta, siempre junto a su texto: gris mientras no sale del celular, morado mientras se espera
 * a Logística (como «Esperando repuesto»), verde al entregarse y rojo si Logística la rechazó.
 */
@Composable
@ReadOnlyComposable
fun colorEstadoSolicitud(estado: EstadoSolicitudVisible): ColorEstado {
    val estados = SigtecTheme.estados
    return when (estado) {
        EstadoSolicitudVisible.POR_ENVIAR -> estados.asignada
        EstadoSolicitudVisible.POR_DESPACHAR, EstadoSolicitudVisible.EN_CAMINO -> estados.esperandoRepuesto
        EstadoSolicitudVisible.ENTREGADA -> estados.solucionado
        EstadoSolicitudVisible.RECHAZADA -> estados.critico
    }
}
