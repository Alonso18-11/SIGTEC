package com.softcorp.sigtec.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
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
    // Crítico se superpone a cualquier marca pendiente, con el ícono de alerta en lugar del punto (DESIGN.md 4)
    if (critica && marca != MarcaSeguimiento.CERRADA) {
        EtiquetaEstado(
            texto = textoCritico(diasPendiente),
            color = estados.critico,
            modifier = modifier,
            icono = Icons.Outlined.ErrorOutline
        )
        return
    }
    val (texto, color) = when (marca) {
        MarcaSeguimiento.SIN_ASIGNAR -> "Sin asignar" to estados.sinAsignar
        MarcaSeguimiento.ASIGNADA -> "Asignada" to estados.asignada
        MarcaSeguimiento.EN_ATENCION -> "En atención" to estados.enAtencion
        MarcaSeguimiento.ESPERANDO_REPUESTO -> textoEsperandoRepuesto(diasPendiente) to estados.esperandoRepuesto
        MarcaSeguimiento.CERRADA -> "Solucionado" to estados.solucionado
    }
    EtiquetaEstado(texto = texto, color = color, modifier = modifier)
}

// Reglas de redacción del retema v2: coma en lugar de punto medio (plan, sección 5.1)
internal fun textoCritico(dias: Long) = "Crítico, ${dias(dias)}"

internal fun textoEsperandoRepuesto(dias: Long) = "Esperando repuesto, ${dias(dias)}"

private fun dias(n: Long) = if (n == 1L) "1 día" else "$n días"
