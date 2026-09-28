package com.softcorp.sigtec.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.softcorp.sigtec.core.domain.usecase.ObservarEstadoDatos
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AvisoConexionViewModel @Inject constructor(
    observarEstadoDatos: ObservarEstadoDatos
) : ViewModel() {
    val estado: StateFlow<EstadoDatos?> = observarEstadoDatos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * HU-05. Se pasa como slot a las pantallas de lista: aviso = { AvisoConexion() }.
 * No ocupa espacio cuando hay conexión y no queda nada por enviar.
 * No usarlo dentro de una @Preview (usa hiltViewModel): ahí va AvisoConexionContenido.
 */
@Composable
fun AvisoConexion(modifier: Modifier = Modifier, vm: AvisoConexionViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    estado?.let { AvisoConexionContenido(it, modifier) }
}

@Composable
fun AvisoConexionContenido(estado: EstadoDatos, modifier: Modifier = Modifier) {
    val pendientes = estado.pendientesPorEnviar
    val texto = when {
        !estado.enLinea -> buildString {
            append("Sin conexión")
            estado.ultimaDescarga?.let { append(" · mostrando lo descargado a las ${it.horaLocal()}") }
            if (pendientes > 0) append(" · ${cambios(pendientes)} por enviar")
        }
        pendientes > 0 -> "Enviando ${cambios(pendientes)}…"
        else -> return
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Espaciado.m), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (!estado.enLinea) Icons.Outlined.WifiOff else Icons.Outlined.CloudUpload,
                contentDescription = null
            )
            Spacer(Modifier.width(Espaciado.s))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun cambios(n: Int) = if (n == 1) "1 cambio" else "$n cambios"

private fun Instant.horaLocal(): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(this)

@Preview(name = "Sin conexión", showBackground = true)
@Composable
private fun AvisoSinConexionPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = false, ultimaDescarga = Instant.now(), pendientesPorEnviar = 2),
        Modifier.padding(Espaciado.m)
    )
}

@Preview(name = "Enviando", showBackground = true)
@Composable
private fun AvisoEnviandoPreview() = SigtecTheme {
    AvisoConexionContenido(
        EstadoDatos(enLinea = true, ultimaDescarga = Instant.now(), pendientesPorEnviar = 1),
        Modifier.padding(Espaciado.m)
    )
}
