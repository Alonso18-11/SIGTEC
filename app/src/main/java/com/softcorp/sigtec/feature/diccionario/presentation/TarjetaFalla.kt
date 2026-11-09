package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado
import com.softcorp.sigtec.core.ui.components.Tarjeta
import com.softcorp.sigtec.feature.incidencias.presentation.etiqueta

/**
 * Una falla del diccionario (prototipo 14, RF-1302): tipo y componente, título, solución y autor. Desplegada muestra
 * también los síntomas y la fecha (RF-1303). [sugerencia] pone la etiqueta «Sugerencia» y no el autor (RF-1327):
 * así se ve en el detalle de una incidencia.
 */
@Composable
fun TarjetaFalla(
    falla: FallaDiccionario,
    modifier: Modifier = Modifier,
    desplegada: Boolean = false,
    sugerencia: Boolean = false,
    alTocar: (() -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    Tarjeta(modifier.fillMaxWidth(), onClick = alTocar) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Espaciado.s), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                if (sugerencia) EtiquetaEstado("Sugerencia", SigtecTheme.estados.solucionado)
                EtiquetaEstado(falla.tipo.etiqueta(), colorTipo(falla.tipo))
                EtiquetaEstado(falla.componente, SigtecTheme.estados.asignada)
            }
            Text(falla.titulo, style = MaterialTheme.typography.titleMedium)
            if (desplegada) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Síntomas: ") }
                        append(falla.sintomas)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Solución: ") }
                    append(falla.solucion)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
            if (!sugerencia) {
                val autor = if (desplegada) {
                    "Registrada por ${falla.autorNombre}, ${fechaRegistro(falla.registradaEn)}"
                } else {
                    "Registrada por ${falla.autorNombre}"
                }
                Text(autor, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}
