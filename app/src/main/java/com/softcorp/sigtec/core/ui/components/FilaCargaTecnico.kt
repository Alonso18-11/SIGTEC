package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.theme.Elevacion
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

private val AltoSegmento = 6.dp

/** RF-06: con la carga en el límite no se le puede asignar más (también lo valida el dominio y Firestore). */
internal fun alcanzoLimite(activas: Int, limite: Int) = activas >= limite

/**
 * Técnico con su carga (DESIGN.md 4, prototipo 07): un segmento por cada unidad del límite de atención. En el límite
 * la fila queda bloqueada, en rojo, con candado y «Alcanzó su límite de atención» (RF-06). [recomendado] pone la
 * etiqueta «Recomendado» (la usa quien tiene menos carga). Elegirla es como un botón de opción.
 */
@Composable
fun FilaCargaTecnico(
    nombre: String,
    activas: Int,
    limite: Int,
    seleccionada: Boolean,
    alElegir: () -> Unit,
    modifier: Modifier = Modifier,
    recomendado: Boolean = false
) {
    val c = MaterialTheme.colorScheme
    val bloqueada = alcanzoLimite(activas, limite)
    val colorCarga = if (bloqueada) c.error else c.primary
    val descripcion = buildString {
        append("$nombre, $activas de $limite activas")
        if (bloqueada) append(". Alcanzó su límite de atención")
        if (recomendado) append(". Recomendado")
    }
    Surface(
        color = if (bloqueada) c.errorContainer else c.surface,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = if (bloqueada) 0.dp else Elevacion.tarjeta,
        border = when {
            seleccionada -> BorderStroke(2.dp, c.primary)
            else -> BorderStroke(1.dp, SigtecTheme.marca.bordeTarjeta)
        },
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, enabled = !bloqueada, role = Role.RadioButton, onClick = alElegir)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
    ) {
        Row(
            Modifier
                .padding(Espaciado.m)
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(Espaciado.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(nombre, tamano = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                    Text(
                        nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (bloqueada) c.onErrorContainer else c.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (recomendado && !bloqueada) EtiquetaEstado("Recomendado", SigtecTheme.estados.solucionado)
                    Text("$activas / $limite", style = MaterialTheme.typography.labelLarge, color = if (bloqueada) c.onErrorContainer else c.onSurface)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                    repeat(limite) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(AltoSegmento)
                                .clip(CircleShape)
                                .background(if (i < activas) colorCarga else c.surfaceVariant)
                        )
                    }
                }
                if (bloqueada) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.onErrorContainer)
                        Text("Alcanzó su límite de atención", style = MaterialTheme.typography.bodySmall, color = c.onErrorContainer)
                    }
                }
            }
            if (!bloqueada) RadioButton(selected = seleccionada, onClick = null)
        }
    }
}

@Composable
private fun FilasCargaDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            FilaCargaTecnico("Jorge Salas", activas = 1, limite = 4, seleccionada = true, alElegir = {}, recomendado = true)
            FilaCargaTecnico("Ana Torres", activas = 3, limite = 5, seleccionada = false, alElegir = {})
            FilaCargaTecnico("Rosa Vilca", activas = 4, limite = 4, seleccionada = false, alElegir = {})
        }
    }
}

@Preview(name = "Carga de técnicos, claro", showBackground = true)
@Composable
private fun CargaClaro() = SigtecTheme { FilasCargaDePrueba() }

@Preview(name = "Carga de técnicos, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CargaOscuro() = SigtecTheme(darkTheme = true) { FilasCargaDePrueba() }
