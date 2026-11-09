package com.softcorp.sigtec.feature.acceso.presentation

import com.softcorp.sigtec.core.ui.components.IconosBarraEstadoClaros
import com.softcorp.sigtec.core.ui.components.CampoTexto
import com.softcorp.sigtec.core.ui.components.CabeceraHero
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.statusBarsPadding
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.DecisionBorrado
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.core.ui.mensaje

private val TamanoLogo = 64.dp
private val TamanoIconoLogo = 32.dp
private val AnchoMaximoFormulario = 420.dp

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
    // el diálogo aparece solo (sección 16.2, paso 4). La key es el evento de un solo uso
    // del ViewModel, no el id estático: así no se relanza al recrearse la Activity (RF-29)
    LaunchedEffect(estado.ofrecerHuellaParaId) {
        if (estado.ofrecerHuellaParaId != null) {
            pedirHuella()
            vm.alHuellaOfrecida()
        }
    }

    LoginScreen(
        estado = estado,
        alCambiarCorreo = vm::alCambiarCorreo,
        alCambiarContrasena = vm::alCambiarContrasena,
        alAlternarVisibilidad = vm::alternarVisibilidad,
        alIngresar = vm::ingresar,
        alIngresarConHuella = pedirHuella,
        alConfirmarIngreso = vm::alConfirmarIngreso,
        alCancelarIngreso = vm::alCancelarIngreso
    )
}

@Composable
fun LoginScreen(
    estado: LoginUiState,
    alCambiarCorreo: (String) -> Unit,
    alCambiarContrasena: (String) -> Unit,
    alAlternarVisibilidad: () -> Unit,
    alIngresar: () -> Unit,
    alIngresarConHuella: () -> Unit,
    alConfirmarIngreso: () -> Unit,
    alCancelarIngreso: () -> Unit
) {
    val foco = LocalFocusManager.current
    val enviar = { foco.clearFocus(); alIngresar() }
    val bloqueada = estado.usuarioBloqueado != null
    val marca = SigtecTheme.marca

    // Pantallas v2 (prototipo 01): cabecera oscura con el logo y el nombre, y el formulario debajo sobre el fondo
    IconosBarraEstadoClaros()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()               // pantallas v2: la raíz ya no aplica el margen superior
            .imePadding()                      // el teclado no tapa el botón
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------- Identidad ----------
        CabeceraHero {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(top = Espaciado.xl, bottom = Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.m)
            ) {
                Surface(
                    color = marca.acento,
                    contentColor = marca.sobreAcento,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.size(TamanoLogo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(TamanoIconoLogo))
                    }
                }
                Text(
                    "SIGTEC",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = marca.sobreHero,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = AnchoMaximoFormulario)
                .padding(horizontal = Espaciado.m, vertical = Espaciado.l)
        ) {
            Text(
                estado.usuarioBloqueado?.let { "Hola de nuevo, ${it.nombre.substringBefore(' ')}" }
                    ?: "Iniciar sesión",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(Espaciado.m))

            // ---------- Correo y contraseña (HU-01) ----------
            CampoTexto(
                valor = estado.correo,
                alCambiar = alCambiarCorreo,
                etiqueta = "Correo institucional",
                icono = Icons.Outlined.Email,
                habilitado = !estado.cargando,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                // Permite que el gestor de contraseñas del celular complete el campo
                modifier = Modifier.semantics { contentType = ContentType.Username }
            )
            Spacer(Modifier.height(Espaciado.sm))

            CampoTexto(
                valor = estado.contrasena,
                alCambiar = alCambiarContrasena,
                etiqueta = "Contraseña",
                icono = Icons.Outlined.Lock,
                final = {
                    IconButton(onClick = alAlternarVisibilidad) {
                        Icon(
                            if (estado.contrasenaVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = if (estado.contrasenaVisible) "Ocultar contraseña"
                            else "Mostrar contraseña"
                        )
                    }
                },
                transformacion = if (estado.contrasenaVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                habilitado = !estado.cargando,
                error = estado.error != null,
                ayuda = estado.error?.mensaje(),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { enviar() }),
                modifier = Modifier.semantics { contentType = ContentType.Password }
            )
            Spacer(Modifier.height(Espaciado.m))

            BotonPrincipal(
                onClick = enviar,
                enabled = estado.puedeIngresar,
                modifier = Modifier.fillMaxWidth()
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
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "o",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Espaciado.m),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ---------- Huella (HU-02) ----------
            BotonSecundario(
                onClick = alIngresarConHuella,
                enabled = bloqueada && !estado.cargando,
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Espaciado.s)
            )

            // Otra persona frente al celular bloqueado: entra con su propia cuenta,
            // sin tocar la sesión ni la huella del dueño
            estado.usuarioBloqueado?.let { usuario ->
                Text(
                    "¿No eres ${usuario.nombre.substringBefore(' ')}? Ingresa con tu correo y contraseña.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Espaciado.s)
                )
            }
        }
    }

    estado.confirmarIngreso?.let { decision ->
        DialogoConfirmarIngreso(
            nombre = estado.usuarioBloqueado?.nombre?.substringBefore(' ') ?: "El usuario anterior",
            decision = decision,
            alConfirmar = alConfirmarIngreso,
            alCancelar = alCancelarIngreso
        )
    }
}

/** Avisa de los cambios sin enviar y las fotos del dueño bloqueado antes de dejar entrar a otra cuenta; tocar fuera cancela. */ // RF-11, RF-944
@Composable
private fun DialogoConfirmarIngreso(
    nombre: String,
    decision: DecisionBorrado.Confirmar,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
        title = { Text("¿Ingresar con otra cuenta?") },
        text = {
            Text(textoAvisoIngreso(nombre, decision.pendientes, decision.fotos))
        },
        confirmButton = {
            BotonTexto(onClick = alConfirmar, color = MaterialTheme.colorScheme.error) { Text("Ingresar de todos modos") }
        },
        dismissButton = { BotonTexto(onClick = alCancelar) { Text("Cancelar") } }
    )
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
        LoginScreen(LoginUiState(), {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Error, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginConError() = SigtecTheme(darkTheme = true) {
    Surface {
        LoginScreen(
            LoginUiState(
                correo = "lramirez@softcorp.pe",
                contrasena = "123456",
                error = ErrorDominio.CredencialesInvalidas
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Bloqueada por el dueño", showBackground = true)
@Composable
private fun LoginBloqueada() = SigtecTheme {
    Surface {
        LoginScreen(LoginUiState(usuarioBloqueado = usuarioDePrueba), {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Diálogo de ingreso, claro", showBackground = true)
@Composable
private fun LoginDialogoIngresoClaro() = SigtecTheme(darkTheme = false) {
    Surface {
        LoginScreen(
            LoginUiState(usuarioBloqueado = usuarioDePrueba, confirmarIngreso = DecisionBorrado.Confirmar(1)),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Diálogo de ingreso, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginDialogoIngresoOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        LoginScreen(
            LoginUiState(usuarioBloqueado = usuarioDePrueba, confirmarIngreso = DecisionBorrado.Confirmar(3, fotos = 2)),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Diálogo de ingreso solo con fotos, claro", showBackground = true)
@Composable
private fun LoginDialogoIngresoFotosClaro() = SigtecTheme(darkTheme = false) {
    Surface {
        LoginScreen(
            LoginUiState(usuarioBloqueado = usuarioDePrueba, confirmarIngreso = DecisionBorrado.Confirmar(0, 1)),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "Diálogo de ingreso solo con fotos, oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginDialogoIngresoFotosOscuro() = SigtecTheme(darkTheme = true) {
    Surface {
        LoginScreen(
            LoginUiState(usuarioBloqueado = usuarioDePrueba, confirmarIngreso = DecisionBorrado.Confirmar(0, 4)),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(name = "El dueño vuelve tras otra sesión", showBackground = true)
@Composable
private fun LoginRegresoDelDueno() = SigtecTheme {
    Surface {
        LoginScreen(LoginUiState(propietarioHuella = "Luis Ramírez"), {}, {}, {}, {}, {}, {}, {})
    }
}