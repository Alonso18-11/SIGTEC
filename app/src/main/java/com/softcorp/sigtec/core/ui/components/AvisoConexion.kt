package com.softcorp.sigtec.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.softcorp.sigtec.core.domain.model.EstadoDatos
import com.softcorp.sigtec.core.domain.usecase.DescartarCambiosRechazados
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AvisoConexionViewModel @Inject constructor(
    observarEstadoDatos: ObservarEstadoDatos,
    private val descartarCambiosRechazados: DescartarCambiosRechazados
) : ViewModel() {
    val estado: StateFlow<EstadoDatos?> = observarEstadoDatos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // RF-80: si Room falla, la marca sigue y el aviso no desaparece; se puede volver a pulsar
    fun alEntendido() {
        viewModelScope.launch { descartarCambiosRechazados() }
    }
}

/**
 * HU-05. Se pasa como slot a las pantallas de lista: aviso = { AvisoConexion() }.
 * No ocupa espacio cuando hay conexión y no queda nada por enviar.
 * No usarlo dentro de una @Preview (usa hiltViewModel): ahí va AvisoConexionContenido.
 */
@Composable
fun AvisoConexion(modifier: Modifier = Modifier, vm: AvisoConexionViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    estado?.let { AvisoConexionContenido(it, modifier, alEntendido = vm::alEntendido) }
}

@Composable
fun AvisoConexionContenido(
    estado: EstadoDatos,
    modifier: Modifier = Modifier,
    alEntendido: () -> Unit = {}
) {
    val texto = textoAviso(estado) ?: return
    val icono = when {
        !estado.enLinea -> Icons.Outlined.WifiOff
        estado.necesitanAtencion > 0 -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.CloudUpload
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Espaciado.m), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null)
            Spacer(Modifier.width(Espaciado.s))
            Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (puedeDescartar(estado)) {
                TextButton(onClick = alEntendido) { Text("Entendido") }
            }
        }
    }
}

/**
 * HU-05 (RF-76, RF-77). Función pura para poder probarla sin Compose.
 * Orden de prioridad: sin conexión, rechazos (RF-75, RF-79), enviando, o nada (no se reserva espacio).
 */
internal fun textoAviso(estado: EstadoDatos): String? {
    val pendientes = estado.pendientesPorEnviar
    return when {
        !estado.enLinea -> buildString {
            append("Sin conexión")
            estado.ultimaDescarga?.let { append(" · mostrando lo descargado a las ${it.horaLocal()}") }
            if (pendientes > 0) append(" · ${cambios(pendientes)} por enviar")
        }
        estado.necesitanAtencion > 0 -> "${cambios(estado.necesitanAtencion)} no se ${pudo(estado.necesitanAtencion)} enviar"
        pendientes > 0 -> "Enviando ${cambios(pendientes)}…"
        else -> null
    }
}

/**
 * RF-80: «Entendido» solo acompaña al texto de rechazos. Sin conexión el aviso habla de la
 * conexión, y un botón que no corresponde al texto confundiría (D-11 de la spec 005).
 */
internal fun puedeDescartar(estado: EstadoDatos): Boolean = estado.enLinea && estado.necesitanAtencion > 0

private fun cambios(n: Int) = if (n == 1) "1 cambio" else "$n cambios"

/** El verbo concuerda con el número de cambios, igual que `textoSePerdera` (HU-01, D-4). */
private fun pudo(n: Int) = if (n == 1) "pudo" else "pudieron"

private fun Instant.horaLocal(): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(this)

@Preview(name = "Sin conexión", showBackground = true)
@Composable
private fun AvisoSinConexionPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = false, ultimaDescarga = Instant.now(), pendientesPorEnviar = 2, necesitanAtencion = 0),
        Modifier.padding(Espaciado.m)
    )
}

@Preview(name = "Enviando", showBackground = true)
@Composable
private fun AvisoEnviandoPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = true, ultimaDescarga = Instant.now(), pendientesPorEnviar = 1, necesitanAtencion = 0),
        Modifier.padding(Espaciado.m)
    )
}

@Preview(name = "No se pudo enviar", showBackground = true)
@Composable
private fun AvisoNecesitaAtencionPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = true, ultimaDescarga = Instant.now(), pendientesPorEnviar = 0, necesitanAtencion = 2),
        Modifier.padding(Espaciado.m)
    )
}

@Preview(name = "No se pudo enviar · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvisoNecesitaAtencionOscuroPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = true, ultimaDescarga = Instant.now(), pendientesPorEnviar = 0, necesitanAtencion = 2),
        Modifier.padding(Espaciado.m)
    )
}
