package com.softcorp.sigtec.feature.acceso.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.mensaje

private val TamanoLogo = 72.dp

@Composable
fun LoginRoute(vm: LoginViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()

    val pedirHuella = {
        verificador.verificar(
            titulo = "Desbloquear SIGTEC",
            subtitulo = estado.usuarioBloqueado?.nombre,
            alTerminar = vm::alVerificarHuella
        )
    }

    // Al abrir la app o al bloquearla, si el dueño de la huella tiene la sesión guardada,
    // el diálogo aparece solo (sección 16.2, paso 4)
    LaunchedEffect(estado.usuarioBloqueado?.id) {
        if (estado.usuarioBloqueado != null) pedirHuella()
    }

    LoginScreen(
        estado = estado,
        alCambiarCorreo = vm::alCambiarCorreo,
        alCambiarContrasena = vm::alCambiarContrasena,
        alAlternarVisibilidad = vm::alternarVisibilidad,
        alIngresar = vm::ingresar,
        alIngresarConHuella = pedirHuella
    )
}

@Composable
fun LoginScreen(
    estado: LoginUiState,
    alCambiarCorreo: (String) -> Unit,
    alCambiarContrasena: (String) -> Unit,
    alAlternarVisibilidad: () -> Unit,
    alIngresar: () -> Unit,
    alIngresarConHuella: () -> Unit
) {
    val foco = LocalFocusManager.current
    val enviar = { foco.clearFocus(); alIngresar() }
    val bloqueada = estado.usuarioBloqueado != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()                      // el teclado no tapa el botón
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espaciado.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Espaciado.xl * 2))

        // ---------- Identidad ----------
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(TamanoLogo)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(Espaciado.l))
        Text(
            "SIGTEC",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Soporte y Sistemas · Soft Corporation",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        estado.usuarioBloqueado?.let { usuario ->
            Spacer(Modifier.height(Espaciado.m))
            Text(
                "Hola de nuevo, ${usuario.nombre.substringBefore(' ')}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(Espaciado.xl * 2))

        // ---------- Correo y contraseña (HU-01) ----------
        OutlinedTextField(
            value = estado.correo,
            onValueChange = alCambiarCorreo,
            label = { Text("Correo institucional") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            singleLine = true,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
            // Permite que el gestor de contraseñas del celular complete el campo
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username }
        )
        Spacer(Modifier.height(Espaciado.m))

        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = alCambiarContrasena,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = alAlternarVisibilidad) {
                    Icon(
                        if (estado.contrasenaVisible) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = if (estado.contrasenaVisible) "Ocultar contraseña"
                        else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (estado.contrasenaVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            singleLine = true,
            enabled = !estado.cargando,
            isError = estado.error != null,
            supportingText = estado.error?.let { error -> { Text(error.mensaje()) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { enviar() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password }
        )
        Spacer(Modifier.height(Espaciado.l))

        Button(
            onClick = enviar,
            enabled = estado.puedeIngresar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AreaTactilMinima)
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Espaciado.l),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Ingresar")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Espaciado.m)
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                "o",
                modifier = Modifier.padding(horizontal = Espaciado.m),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier.weight(1f))
        }

        // ---------- Huella (HU-02) ----------
        OutlinedButton(
            onClick = alIngresarConHuella,
            enabled = bloqueada && !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AreaTactilMinima)
        ) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(Espaciado.s))
            Text("Ingresar con huella")
        }

        val textoHuella = when {
            estado.avisoHuella != null -> estado.avisoHuella
            bloqueada -> "Toca para desbloquear con tu huella."
            estado.propietarioHuella != null ->
                "${estado.propietarioHuella.substringBefore(' ')}: ingresa con tu contraseña " +
                        "para volver a usar tu huella."
            else -> "Disponible si activaste la huella en Ajustes de seguridad."
        }
        Text(
            textoHuella,
            style = MaterialTheme.typography.bodySmall,
            color = if (estado.avisoHuella != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Espaciado.s)
        )

        // Otra persona frente al celular bloqueado: entra con su propia cuenta,
        // sin tocar la sesión ni la huella del dueño
        estado.usuarioBloqueado?.let { usuario ->
            Text(
                "¿No eres ${usuario.nombre.substringBefore(' ')}? Ingresa con tu correo y contraseña.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Espaciado.s)
            )
        }

        Spacer(Modifier.height(Espaciado.xl * 2))
        Text(
            "Acceso exclusivo para el personal del área de Soporte y Sistemas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Espaciado.l))
    }
}

// ---------- Vistas previas: la pantalla se prueba sin Firebase ni ViewModel ----------

private val usuarioDePrueba = Usuario(
    id = "1",
    nombre = "Luis Ramírez",
    correo = "lramirez@softcorp.pe",
    area = "Soporte y Sistemas",
    cargo = "Técnico de soporte",
    perfil = Perfil.TECNICO
)

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun LoginVacio() = SigtecTheme {
    Surface {
        LoginScreen(LoginUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Error · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginConError() = SigtecTheme(darkTheme = true) {
    Surface {
        LoginScreen(
            LoginUiState(
                correo = "lramirez@softcorp.pe",
                contrasena = "123456",
                error = ErrorDominio.CredencialesInvalidas
            ),
            {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Bloqueada por el dueño", showBackground = true)
@Composable
private fun LoginBloqueada() = SigtecTheme {
    Surface {
        LoginScreen(LoginUiState(usuarioBloqueado = usuarioDePrueba), {}, {}, {}, {}, {})
    }
}

@Preview(name = "El dueño vuelve tras otra sesión", showBackground = true)
@Composable
private fun LoginRegresoDelDueno() = SigtecTheme {
    Surface {
        LoginScreen(LoginUiState(propietarioHuella = "Luis Ramírez"), {}, {}, {}, {}, {})
    }
}