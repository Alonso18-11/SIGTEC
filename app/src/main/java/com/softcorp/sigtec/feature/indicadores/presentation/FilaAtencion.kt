package com.softcorp.sigtec.feature.indicadores.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.feature.indicadores.domain.AtencionEquipo
import java.time.Instant

private val TamanoPunto = 12.dp
private val AnchoColumna = 24.dp
private val AnchoLinea = 2.dp
private const val OPACIDAD_HALO = 0.35f

/**
 * HU-16 (D-8, RF-1615 a RF-1620): una atención del historial con su punto y la línea hacia la siguiente. Fila propia
 * porque el prototipo 11 tiene tres niveles de texto y `LineaTiempo` (dos niveles) la usa el detalle. Toda la fila es
 * un botón que abre el detalle de la incidencia, y TalkBack la lee como una sola frase.
 */
@Composable
fun FilaAtencion(
    atencion: AtencionEquipo,
    ahora: Instant,
    ultima: Boolean,
    alAbrir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    val descripcion = descripcionAtencion(atencion)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = AreaTactilMinima)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClickLabel = "Abrir incidencia", onClick = alAbrir)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
    ) {
        Column(
            Modifier
                .width(AnchoColumna)
                .fillMaxHeight()
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Punto(cerrada = atencion.cerrada, Modifier.padding(top = Espaciado.xs))
            if (!ultima) {
                Box(
                    Modifier
                        .padding(vertical = Espaciado.xs)
                        .width(AnchoLinea)
                        .weight(1f)
                        .background(c.outlineVariant)
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = Espaciado.sm, bottom = if (ultima) 0.dp else Espaciado.m)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Text(
                    encabezadoAtencion(atencion),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // RF-1616: la misma etiqueta de la lista, con texto (no depende solo del color)
                EtiquetaIncidencia(atencion.marca, atencion.critica, diasDesdeRegistro(atencion, ahora))
            }
            Text(atencion.descripcion, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            trabajoAtencion(atencion)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            Text(tecnicoAtencion(atencion), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

/** RF-1619: verde las cerradas; ámbar con halo las pendientes, como «en curso» de `LineaTiempo`. */
@Composable
private fun Punto(cerrada: Boolean, modifier: Modifier = Modifier) {
    if (cerrada) {
        Box(modifier.size(TamanoPunto).clip(CircleShape).background(SigtecTheme.estados.solucionado.punto))
    } else {
        val acento = SigtecTheme.marca.acento
        Box(
            modifier
                .size(TamanoPunto + 8.dp)
                .clip(CircleShape)
                .background(acento.copy(alpha = OPACIDAD_HALO)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(TamanoPunto).clip(CircleShape).background(acento))
        }
    }
}

// ---------- Vistas previas ----------

private val ahoraPrevia = Instant.parse("2026-09-28T15:00:00Z")

internal val atencionesDePrueba = listOf(
    AtencionEquipo(
        "1", "INC-0124", Instant.parse("2026-09-27T14:30:00Z"), "No enciende después del corte de luz",
        MarcaSeguimiento.EN_ATENCION, "Luis Ramírez", null
    ),
    AtencionEquipo(
        "2", "INC-0098", Instant.parse("2026-09-02T15:00:00Z"), "Lentitud al abrir el sistema contable",
        MarcaSeguimiento.CERRADA, "Rosa Vilca", "Se amplió la memoria RAM a 16 GB."
    ),
    AtencionEquipo(
        "3", "INC-0071", Instant.parse("2026-08-18T15:00:00Z"), "Disco duro con sectores dañados",
        MarcaSeguimiento.CERRADA, "Jorge Salas", null
    ),
    AtencionEquipo(
        "4", null, Instant.parse("2026-07-01T15:00:00Z"), "El teclado no responde",
        MarcaSeguimiento.SIN_ASIGNAR, null, null, critica = true
    )
)

@Composable
private fun FilasDePrueba() = Surface(color = MaterialTheme.colorScheme.background) {
    Column(Modifier.padding(Espaciado.m)) {
        atencionesDePrueba.forEachIndexed { i, a ->
            FilaAtencion(a, ahoraPrevia, ultima = i == atencionesDePrueba.lastIndex, alAbrir = {})
        }
    }
}

@Preview(name = "Atenciones, claro", showBackground = true)
@Composable
private fun FilasClaro() = SigtecTheme { FilasDePrueba() }

@Preview(name = "Atenciones, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilasOscuro() = SigtecTheme(darkTheme = true) { FilasDePrueba() }
