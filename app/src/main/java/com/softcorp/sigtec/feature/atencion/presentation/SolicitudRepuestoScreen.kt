package com.softcorp.sigtec.feature.atencion.presentation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.core.ui.mensaje
import com.softcorp.sigtec.feature.atencion.domain.Disponibilidad
import com.softcorp.sigtec.feature.atencion.domain.PiezaLogistica
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import java.time.Instant

@Composable
fun SolicitudRepuestoRoute(
    alVolver: () -> Unit,
    alRegistrada: () -> Unit,
    vm: SolicitudRepuestoViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()

    // RF-1114: se vuelve al detalle una sola vez
    LaunchedEffect(estado.registradaId) {
        if (estado.registradaId != null) {
            vm.alVolverAlDetalle()
            alRegistrada()
        }
    }

    SolicitudRepuestoScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarPieza = vm::alCambiarPieza,
        alElegirPieza = vm::alElegirPieza,
        alReintentar = vm::alReintentar,
        alCambiarMotivo = vm::alCambiarMotivo,
        // RF-1111 y RF-1112: huella o PIN; si se cancela, no se guarda nada
        alEnviar = {
            verificador.verificar(titulo = "Confirma la solicitud de repuesto") { resultado ->
                if (resultado is ResultadoBiometrico.Exito) vm.alConfirmar(resultado.instante)
            }
        }
    )
}

/** HU-11 (prototipo 8): consultar la pieza, escribir el motivo y enviar con huella. Solo dibuja. */
@Composable
fun SolicitudRepuestoScreen(
    estado: SolicitudRepuestoUiState,
    alVolver: () -> Unit,
    alCambiarPieza: (String) -> Unit,
    alElegirPieza: (PiezaLogistica) -> Unit,
    alReintentar: () -> Unit,
    alCambiarMotivo: (String) -> Unit,
    alEnviar: () -> Unit
) {
    val editable = !estado.enviando
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Solicitar repuesto", alVolver = alVolver) },
        bottomBar = { PieEnviar(estado, alEnviar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            estado.incidencia?.let { ParaLaIncidencia(it) }

            OutlinedTextField(
                value = estado.pieza,
                onValueChange = alCambiarPieza,
                label = { Text("Pieza requerida *") },
                supportingText = { Text("Escribe al menos 3 letras para consultar a Logística") },
                singleLine = true,
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (estado.coincidencias.isNotEmpty()) {
                Coincidencias(estado.coincidencias, alElegirPieza)
            }
            estado.disponibilidad?.let { TarjetaDisponibilidad(it, alReintentar) }

            OutlinedTextField(
                value = estado.motivo,
                onValueChange = alCambiarMotivo,
                label = { Text("Motivo *") },
                supportingText = { Text("Qué le pasa al equipo y por qué hace falta la pieza") },
                minLines = 3,
                enabled = editable,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(TamanoIconoChico)
                )
                Text(
                    "Mientras llega la pieza, la incidencia seguirá Pendiente con la marca «Esperando repuesto» " +
                        "y el conteo de días.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ParaLaIncidencia(incidencia: Incidencia) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Text(
                "Para la incidencia",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${incidencia.codigoVisible ?: POR_SINCRONIZAR} · ${incidencia.codigoEquipo}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                incidencia.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** RF-1104: las piezas del catálogo que coinciden, para elegir una. */
@Composable
private fun Coincidencias(piezas: List<PiezaLogistica>, alElegir: (PiezaLogistica) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            piezas.forEachIndexed { i, pieza ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AreaTactilMinima)
                        .clickable { alElegir(pieza) }
                        .padding(horizontal = Espaciado.m, vertical = Espaciado.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)
                ) {
                    Icon(
                        Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(TamanoIconoChico)
                    )
                    Text(pieza.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** RF-1105 a RF-1108: lo que respondió Logística, con el color de estado y siempre con texto e ícono. */
@Composable
private fun TarjetaDisponibilidad(disponibilidad: Disponibilidad, alReintentar: () -> Unit) {
    val texto = textoDisponibilidad(disponibilidad)
    if (texto == null) {   // RF-1106: consultando, sin congelar la pantalla
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            CircularProgressIndicator(modifier = Modifier.size(TamanoIconoChico), strokeWidth = 2.dp)
            Text(
                "Consultando a Logística…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val estados = SigtecTheme.estados
    val (icono, contenido, fondo) = when (disponibilidad) {
        is Disponibilidad.ConStock -> Triple(Icons.Outlined.CheckCircle, estados.solucionado.contenido, estados.solucionado.contenedor)
        is Disponibilidad.SinStock -> Triple(Icons.Outlined.Inventory2, estados.advertencia.contenido, estados.advertencia.contenedor)
        Disponibilidad.NoEnCatalogo -> Triple(Icons.Outlined.SearchOff, estados.asignada.contenido, estados.asignada.contenedor)
        else -> Triple(Icons.Outlined.CloudOff, estados.asignada.contenido, estados.asignada.contenedor)
    }
    Tarjeta(icono, contenido, fondo, texto) {
        if (disponibilidad == Disponibilidad.SinRespuesta) {
            BotonTexto(onClick = alReintentar, color = contenido) { Text("Reintentar") }   // RF-1108
        }
    }
}

@Composable
private fun Tarjeta(
    icono: ImageVector,
    contenido: Color,
    fondo: Color,
    texto: TextoDisponibilidad,
    acciones: @Composable () -> Unit
) {
    Surface(color = fondo, contentColor = contenido, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Espaciado.m), horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(TamanoIcono))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
                Text(texto.titulo, style = MaterialTheme.typography.titleSmall)
                texto.detalle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                acciones()
            }
        }
    }
}

/** RF-1110 y RF-1111: «Enviar con huella», con lo que le falta al celular si no tiene seguridad. */
@Composable
private fun PieEnviar(estado: SolicitudRepuestoUiState, alEnviar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.sm),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                BotonPrincipal(onClick = alEnviar, enabled = estado.puedeEnviar, modifier = Modifier.fillMaxWidth()) {
                    if (estado.enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(TamanoIconoChico),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(TamanoIconoChico))
                        Spacer(Modifier.width(Espaciado.s))
                        Text("Enviar con huella")
                    }
                }
                val (aviso, color) = if (estado.disponibilidadBiometrica == DisponibilidadBiometrica.SIN_SEGURIDAD) {
                    "Configura un bloqueo de pantalla en tu celular para poder enviar la solicitud." to
                        MaterialTheme.colorScheme.error
                } else {
                    "Se te pedirá tu huella o el bloqueo de pantalla para confirmar." to
                        MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(aviso, style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                estado.error?.let {
                    Text(
                        it.mensaje(),   // RF-1115
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val TamanoIcono = 24.dp
private val TamanoIconoChico = 18.dp

// ---------- Vistas previas ----------

private val incidenciaDePrueba = Incidencia(
    id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende después del corte de luz",
    usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-11-03T09:15:00Z"),
    registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
    marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
)

private val base = SolicitudRepuestoUiState(
    incidencia = incidenciaDePrueba,
    disponibilidadBiometrica = DisponibilidadBiometrica.HUELLA
)

private class EstadosDePrueba : PreviewParameterProvider<SolicitudRepuestoUiState> {
    override val values = sequenceOf(
        base.copy(
            pieza = "fuen",
            coincidencias = listOf(
                PiezaLogistica("1", "Fuente de poder ATX 500 W", 3, 1),
                PiezaLogistica("11", "Fuente de poder ATX 650 W", 0, 4)
            )
        ),
        base.copy(
            pieza = "Fuente de poder ATX 500 W",
            disponibilidad = Disponibilidad.ConStock(3, 1),
            motivo = "La fuente no entrega energía tras el corte de luz. Con una fuente de prueba el equipo enciende."
        ),
        base.copy(pieza = "Mouse óptico USB", disponibilidad = Disponibilidad.SinStock(3)),
        base.copy(pieza = "Placa madre H610", disponibilidad = Disponibilidad.SinRespuesta),
        base.copy(pieza = "Disco", disponibilidad = Disponibilidad.Consultando,
            disponibilidadBiometrica = DisponibilidadBiometrica.SIN_SEGURIDAD)
    )
}

@Composable
private fun Previa(estado: SolicitudRepuestoUiState) =
    SolicitudRepuestoScreen(estado, {}, {}, {}, {}, {}, {})

@Preview(name = "Solicitar repuesto · claro", showBackground = true, heightDp = 820)
@Composable
private fun SolicitudClaro(@PreviewParameter(EstadosDePrueba::class) estado: SolicitudRepuestoUiState) =
    SigtecTheme { Previa(estado) }

@Preview(name = "Solicitar repuesto · oscuro", showBackground = true, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SolicitudOscuro(@PreviewParameter(EstadosDePrueba::class) estado: SolicitudRepuestoUiState) =
    SigtecTheme(darkTheme = true) { Previa(estado) }
