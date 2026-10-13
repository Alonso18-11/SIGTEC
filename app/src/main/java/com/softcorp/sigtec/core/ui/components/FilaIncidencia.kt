package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import java.util.Locale

/**
 * Fila de incidencia de la lista general y de «Mis tareas», al estilo de la lista de issues de Jira:
 * plana, de borde a borde, con una línea divisoria debajo. Recibe los textos ya armados, porque
 * core/ui no depende de feature/ (`codigoParaMostrar`, `fechaCorta` y `diasPendiente` viven ahí).
 */
@Composable
fun FilaIncidencia(
    codigo: String,
    equipo: String,
    descripcion: String,
    marca: MarcaSeguimiento,
    critica: Boolean,
    diasPendiente: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fecha: String? = null,
    tecnico: String? = null,       // con avatar; null si no tiene o no hace falta (en «Mis tareas» son todas suyas)
    sinTecnico: String? = null     // texto atenuado, sin avatar, cuando la incidencia aún no tiene técnico
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AreaTactilMinima)
                .clickable(onClick = onClick)
                .padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$codigo · $equipo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                fecha?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                descripcion,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaIncidencia(marca = marca, critica = critica, diasPendiente = diasPendiente)
                Box(Modifier.weight(1f))
                if (tecnico == null && sinTecnico != null) {
                    Text(
                        sinTecnico,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                tecnico?.let {
                    Avatar(nombre = it, tamano = 20.dp)
                    Box(Modifier.width(Espaciado.xs))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Círculo con las iniciales de una persona, como los avatares de Jira. */
@Composable
fun Avatar(nombre: String, modifier: Modifier = Modifier, tamano: Dp = 32.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciales(nombre),
            style = when {
                tamano < 28.dp -> MaterialTheme.typography.labelSmall
                tamano < 40.dp -> MaterialTheme.typography.labelMedium
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

/** «Luis Ramírez» → «LR»; «Sin técnico» → «ST». Solo letras, como máximo dos. */
internal fun iniciales(nombre: String): String =
    nombre.split(' ')
        .mapNotNull { palabra -> palabra.firstOrNull { it.isLetter() } }
        .take(2)
        .joinToString("")
        .uppercase(Locale.forLanguageTag("es-PE"))
        .ifEmpty { "?" }

@Composable
private fun FilasDePrueba() {
    Surface {
        Column {
            FilaIncidencia(
                "INC-0124", "PC-CONT-014", "No enciende después del corte de luz",
                MarcaSeguimiento.EN_ATENCION, critica = false, diasPendiente = 0, onClick = {},
                fecha = "Hoy 10:42", tecnico = "Luis Ramírez"
            )
            FilaIncidencia(
                "INC-0121", "LAP-GER-005", "Sin conexión a la red Wi-Fi desde la mañana en la oficina de gerencia",
                MarcaSeguimiento.ASIGNADA, critica = true, diasPendiente = 6, onClick = {},
                fecha = "21/09", tecnico = "Ana Torres"
            )
            FilaIncidencia(
                "Por sincronizar", "IMP-RRHH-002", "La impresora no aparece en la red",
                MarcaSeguimiento.SIN_ASIGNAR, critica = false, diasPendiente = 0, onClick = {}, fecha = "Hoy 09:15",
                sinTecnico = "Sin técnico"
            )
        }
    }
}

@Preview(name = "Filas · claro", showBackground = true)
@Composable
private fun FilasClaro() = SigtecTheme { FilasDePrueba() }

@Preview(name = "Filas · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilasOscuro() = SigtecTheme(darkTheme = true) { FilasDePrueba() }
