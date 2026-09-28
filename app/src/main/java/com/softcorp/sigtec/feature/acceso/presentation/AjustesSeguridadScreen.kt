package com.softcorp.sigtec.feature.acceso.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme

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
        alCerrarSesion = vm::salir
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesSeguridadScreen(
    estado: AjustesUiState,
    alVolver: () -> Unit,
    alCambiarHuella: (Boolean) -> Unit,
    alRegistrarHuella: () -> Unit,
    alBloquear: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    // La raíz ya aplica los márgenes de la barra de estado: aquí van en cero para no duplicarlos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de seguridad") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
        ) {
            estado.usuario?.let { TarjetaUsuario(it) }

            // ---------- Acceso ----------
            TituloSeccion("Acceso")
            if (estado.huellaDeOtraPersona) {
                FilaInformativa(
                    Icons.Outlined.Fingerprint,
                    "Ingresar con huella",
                    "La huella de este celular está vinculada a ${estado.preferencias.propietarioNombre}. " +
                            "Solo esa persona puede desactivarla."
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Column(Modifier.weight(1f).padding(horizontal = Espaciado.m)) {
                        Text("Ingresar con huella", style = MaterialTheme.typography.bodyLarge)
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
                EstadoSensor(estado.disponibilidad, alRegistrarHuella)
            }

            // ---------- Confirmaciones ----------
            HorizontalDivider()
            TituloSeccion("Confirmaciones")
            FilaInformativa(
                Icons.Outlined.Lock,
                "Cierre de incidencias y solicitudes de repuesto",
                "Siempre se confirman con tu huella o, si no hay sensor, con el bloqueo de pantalla " +
                        "del celular. Esta opción no se puede desactivar."
            )

            // ---------- Privacidad ----------
            HorizontalDivider()
            TituloSeccion("Privacidad")
            FilaInformativa(
                Icons.Outlined.Shield,
                null,
                "SIGTEC no guarda ni envía tu huella. Android solo le indica a la aplicación " +
                        "si la verificación fue correcta."
            )

            // ---------- Salida ----------
            Spacer(Modifier.height(Espaciado.l))
            if (estado.esPropietario) {
                Button(
                    onClick = alBloquear,
                    modifier = Modifier.fillMaxWidth().heightIn(min = AreaTactilMinima)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                    Spacer(Modifier.width(Espaciado.s))
                    Text("Bloquear SIGTEC")
                }
                NotaSalida("Tu sesión se conserva: vuelves a entrar con tu huella.")
            }
            OutlinedButton(
                onClick = alCerrarSesion,
                modifier = Modifier.fillMaxWidth().heightIn(min = AreaTactilMinima),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
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

@Composable
private fun TarjetaUsuario(usuario: Usuario) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Espaciado.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(TamanoAvatar)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        usuario.nombre.split(" ").take(2).map { it.first() }.joinToString(""),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
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

@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun FilaInformativa(icono: ImageVector, titulo: String?, texto: String) {
    Row {
        Icon(icono, contentDescription = null)
        Column(Modifier.padding(start = Espaciado.m)) {
            titulo?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
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
                    SigtecTheme.estados.esperandoRepuesto.contenido
        DisponibilidadBiometrica.SIN_SEGURIDAD ->
            "Configura un bloqueo de pantalla en tu celular para usar esta opción" to
                    MaterialTheme.colorScheme.error
    }
    Column {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
        if (disponibilidad == DisponibilidadBiometrica.SIN_HUELLA_REGISTRADA) {
            TextButton(onClick = alRegistrarHuella) { Text("Registrar una huella") }
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
            {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Otra persona en el celular", showBackground = true)
@Composable
private fun AjustesOtraPersona() = SigtecTheme {
    Surface {
        AjustesSeguridadScreen(
            AjustesUiState(ana, huellaDeLuis, DisponibilidadBiometrica.HUELLA),
            {}, {}, {}, {}, {}
        )
    }
}