package com.softcorp.sigtec.feature.acceso.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.biometric.intentRegistrarHuella
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.PreferenciasHuella
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.Avatar
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPeligro
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.core.ui.components.Seccion

private val TamanoAvatar = 48.dp

@Composable
fun AjustesSeguridadRoute(alVolver: () -> Unit, vm: AjustesSeguridadViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        vm.reevaluar()
        onPauseOrDispose { }
    }

    AjustesSeguridadScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarHuella = { activar ->
            if (activar) {
                verificador.verificar(titulo = "Activa el ingreso con huella") { resultado ->
                    if (resultado is ResultadoBiometrico.Exito) vm.cambiarHuella(true)
                }
            } else {
                vm.cambiarHuella(false)
            }
        },
        alRegistrarHuella = {
            // Algunos fabricantes no implementan la pantalla directa de registro
            runCatching { context.startActivity(intentRegistrarHuella()) }
                .onFailure { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        },
        alBloquear = vm::bloquear,
        alCerrarSesion = vm::alPulsarCerrarSesion,
        alConfirmarSalida = vm::alConfirmarSalida,
        alCancelarSalida = vm::alCancelarSalida
    )
}

@Composable
fun AjustesSeguridadScreen(
    estado: AjustesUiState,
    alVolver: () -> Unit,
    alCambiarHuella: (Boolean) -> Unit,
    alRegistrarHuella: () -> Unit,
    alBloquear: () -> Unit,
    alCerrarSesion: () -> Unit,
    alConfirmarSalida: () -> Unit,
    alCancelarSalida: () -> Unit
) {
    // La raíz ya aplica los márgenes de la barra de estado; BarraSuperior ya los trae en cero
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Ajustes de seguridad", alVolver = alVolver) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.l)
        ) {
            estado.usuario?.let { TarjetaUsuario(it) }

            // ---------- Acceso ----------
            Seccion("Acceso") {
                Panel {
                    if (estado.huellaDeOtraPersona) {
                        FilaInformativa(
                            Icons.Outlined.Fingerprint,
                            "Ingresar con huella",
                            "La huella de este celular está vinculada a ${estado.preferencias.propietarioNombre}. " +
                                    "Solo esa persona puede desactivarla."
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f).padding(horizontal = Espaciado.m)) {
                                Text("Ingresar con huella", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Vincula este celular a tu cuenta: al bloquear SIGTEC o al abrirla, " +
                                            "entras con tu huella.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.esPropietario,
                                onCheckedChange = alCambiarHuella,
                                enabled = estado.esPropietario || estado.puedeActivar
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EstadoSensor(estado.disponibilidad, alRegistrarHuella)
                    }
                }
            }

            // ---------- Confirmaciones ----------
            Seccion("Confirmaciones") {
                Panel {
                    FilaInformativa(
                        Icons.Outlined.Lock,
                        "Cierre de incidencias y solicitudes de repuesto",
                        "Siempre se confirman con tu huella o, si no hay sensor, con el bloqueo de pantalla " +
                                "del celular. Esta opción no se puede desactivar."
                    )
                }
            }

            // ---------- Privacidad ----------
            Seccion("Privacidad") {
                Panel {
                    FilaInformativa(
                        Icons.Outlined.Shield,
                        null,
                        "SIGTEC no guarda ni envía tu huella. Android solo le indica a la aplicación " +
                                "si la verificación fue correcta."
                    )
                }
            }

            // ---------- Salida ----------
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                if (estado.esPropietario) {
                    BotonSecundario(onClick = alBloquear, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Espaciado.s))
                        Text("Bloquear SIGTEC")
                    }
                    NotaSalida("Tu sesión se conserva: vuelves a entrar con tu huella.")
                    Spacer(Modifier.height(Espaciado.s))
                }
                BotonPeligro(
                    onClick = alCerrarSesion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Cerrar sesión")
                }
                NotaSalida(
                    if (estado.esPropietario)
                        "Se borran los datos de este celular y se desvincula tu huella. " +
                                "Úsalo si el celular pasará a otra persona."
                    else "Se borran de este celular los datos de tu sesión."
                )
            }
        }
    }

    estado.confirmarSalida?.let { pendientes ->
        DialogoConfirmarSalida(
            pendientes = pendientes,
            alConfirmar = alConfirmarSalida,
            alCancelar = alCancelarSalida
        )
    }
}

/** Avisa de cambios sin enviar antes de cerrar sesión; tocar fuera cancela. */ // RF-13
@Composable
private fun DialogoConfirmarSalida(pendientes: Int, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Tienes ${textoCambiosSinEnviar(pendientes)}. Si cierras sesión ahora, ${textoSePerdera(pendientes)}.") },
        confirmButton = {
            BotonTexto(onClick = alConfirmar, color = MaterialTheme.colorScheme.error) { Text("Cerrar de todos modos") }
        },
        dismissButton = { BotonTexto(onClick = alCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun TarjetaUsuario(usuario: Usuario) {
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(usuario.nombre, tamano = TamanoAvatar)
            Column(Modifier.padding(start = Espaciado.m)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                Text(usuario.cargo, style = MaterialTheme.typography.bodyMedium)
                Text(
                    usuario.correo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Grupo de ajustes en un panel blanco con borde fino, como la configuración de Jira. */
@Composable
private fun Panel(contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm),
            content = contenido
        )
    }
}

@Composable
private fun FilaInformativa(icono: ImageVector, titulo: String?, texto: String) {
    Row {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = Espaciado.m)) {
            titulo?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotaSalida(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EstadoSensor(disponibilidad: DisponibilidadBiometrica, alRegistrarHuella: () -> Unit) {
    val (texto, color) = when (disponibilidad) {
        DisponibilidadBiometrica.HUELLA ->
            "Sensor de huella disponible en este celular" to SigtecTheme.estados.solucionado.contenido
        DisponibilidadBiometrica.SOLO_BLOQUEO_PANTALLA ->
            "Este celular no tiene sensor: se usará el PIN, patrón o contraseña del celular" to
                    MaterialTheme.colorScheme.onSurfaceVariant
        DisponibilidadBiometrica.SIN_HUELLA_REGISTRADA ->
            "Tu celular tiene sensor, pero no hay ninguna huella registrada" to
                    SigtecTheme.estados.advertencia.contenido
        DisponibilidadBiometrica.SIN_SEGURIDAD ->
            "Configura un bloqueo de pantalla en tu celular para usar esta opción" to
                    MaterialTheme.colorScheme.error
    }
    Column {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
        if (disponibilidad == DisponibilidadBiometrica.SIN_HUELLA_REGISTRADA) {
            BotonTexto(onClick = alRegistrarHuella, modifier = Modifier.offset(x = -Espaciado.s)) { Text("Registrar una huella") }
        }
    }
}

// ---------- Vistas previas ----------

private val luis = Usuario("t1", "Luis Ramírez", "lramirez@softcorp.pe",
    "Soporte y Sistemas", "Técnico de soporte", Perfil.TECNICO)
private val ana = Usuario("t2", "Ana Torres", "atorres@softcorp.pe",
    "Soporte y Sistemas", "Técnico de soporte", Perfil.TECNICO)
private val huellaDeLuis = PreferenciasHuella("t1", "Luis Ramírez", ofrecida = true)

@Preview(name = "Dueño de la huella", showBackground = true)
@Composable
private fun AjustesDueno() = SigtecTheme {
    Surface {
        AjustesSeguridadScreen(
            AjustesUiState(luis, huellaDeLuis, DisponibilidadBiometrica.HUELLA),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Otra persona en el celular", showBackground = true)
@Composable
private fun AjustesOtraPersona() = SigtecTheme {
    Surface {
        AjustesSeguridadScreen(
            AjustesUiState(ana, huellaDeLuis, DisponibilidadBiometrica.HUELLA),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Diálogo de salida - claro", showBackground = true)
@Composable
private fun DialogoSalidaClaro() = SigtecTheme(darkTheme = false) {
    Surface {
        AjustesSeguridadScreen(
            AjustesUiState(luis, huellaDeLuis, DisponibilidadBiometrica.HUELLA, confirmarSalida = 1),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Diálogo de salida - oscuro", showBackground = true)
@Composable
private fun DialogoSalidaOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        AjustesSeguridadScreen(
            AjustesUiState(luis, huellaDeLuis, DisponibilidadBiometrica.HUELLA, confirmarSalida = 3),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}