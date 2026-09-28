package com.softcorp.sigtec.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.SigtecTheme

/** La etiqueta que se ve en la lista, la bandeja y el detalle. */
@Composable
fun EtiquetaIncidencia(
    marca: MarcaSeguimiento,
    critica: Boolean,
    diasPendiente: Long,
    modifier: Modifier = Modifier
) {
    val estados = SigtecTheme.estados
    val (texto, color) = when {
        // Crítico se superpone a cualquier marca pendiente, como en el prototipo
        critica && marca != MarcaSeguimiento.CERRADA ->
            "Caso crítico · $diasPendiente días" to estados.critico
        else -> when (marca) {
            MarcaSeguimiento.SIN_ASIGNAR -> "Sin asignar" to estados.sinAsignar
            MarcaSeguimiento.ASIGNADA -> "Asignada" to ColorEstado(
                contenido = MaterialTheme.colorScheme.onSurfaceVariant,
                contenedor = MaterialTheme.colorScheme.surfaceVariant
            )
            MarcaSeguimiento.EN_ATENCION -> "En atención" to estados.enAtencion
            MarcaSeguimiento.ESPERANDO_REPUESTO ->
                "Esperando repuesto · $diasPendiente días" to estados.esperandoRepuesto
            MarcaSeguimiento.CERRADA -> "Solucionado" to estados.solucionado
        }
    }
    EtiquetaEstado(texto = texto, color = color, modifier = modifier)
}