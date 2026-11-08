package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

// Medio espacio entre tarjetas arriba y abajo, para que dos seguidas queden a 12 dp sin tocar las listas
private val MargenVertical = 6.dp
private val TamanoAvatar = 28.dp

/**
 * Incidencia de la lista general y de «Mis tareas», como tarjeta v2 (DESIGN.md 4, prototipo 03): código y fecha
 * arriba, título, y abajo la etiqueta con el técnico. Trae su propio margen, así las listas no cambian. Recibe los
 * textos ya armados, porque core/ui no depende de feature/ (`codigoParaMostrar`, `fechaCorta` y `diasPendiente`
 * viven ahí).
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
    tecnico: String? = null,                 // con avatar; null si no tiene o no hace falta
    sinTecnico: String? = null,              // cuando la incidencia aún no tiene técnico
    tecnicoEsUsuarioActual: Boolean = false  // su avatar va en acento
) {
    Tarjeta(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.m, vertical = MargenVertical)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$codigo - $equipo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                fecha?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                descripcion,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaIncidencia(marca = marca, critica = critica, diasPendiente = diasPendiente)
                Spacer(Modifier.width(Espaciado.s))
                Spacer(Modifier.weight(1f))
                if (tecnico == null && sinTecnico != null) {
                    Text(sinTecnico, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tecnico?.let {
                    Avatar(nombre = it, tamano = TamanoAvatar, esUsuarioActual = tecnicoEsUsuarioActual)
                    Spacer(Modifier.width(Espaciado.s))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FilasDePrueba() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(vertical = Espaciado.s)) {
            FilaIncidencia(
                "INC-0124", "PC-CONT-014", "No enciende después del corte de luz",
                MarcaSeguimiento.EN_ATENCION, critica = false, diasPendiente = 0, onClick = {},
                fecha = "Hoy 10:42", tecnico = "Luis Ramírez", tecnicoEsUsuarioActual = true
            )
            FilaIncidencia(
                "INC-0121", "LAP-GER-005", "Sin conexión a la red Wi-Fi desde la mañana en la oficina de gerencia",
                MarcaSeguimiento.ASIGNADA, critica = true, diasPendiente = 6, onClick = {},
                fecha = "21/09", tecnico = "Ana Torres"
            )
            FilaIncidencia(
                "INC-0126", "PC-VENT-031", "Pantalla azul al iniciar Windows",
                MarcaSeguimiento.ESPERANDO_REPUESTO, critica = false, diasPendiente = 4, onClick = {},
                fecha = "Hoy 09:58", tecnico = "Ana Torres"
            )
            FilaIncidencia(
                "Por sincronizar", "IMP-RRHH-002", "La impresora no aparece en la red",
                MarcaSeguimiento.SIN_ASIGNAR, critica = false, diasPendiente = 0, onClick = {}, fecha = "Hoy 09:15",
                sinTecnico = "Sin técnico"
            )
        }
    }
}

@Preview(name = "Filas, claro", showBackground = true)
@Composable
private fun FilasClaro() = SigtecTheme { FilasDePrueba() }

@Preview(name = "Filas, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilasOscuro() = SigtecTheme(darkTheme = true) { FilasDePrueba() }
