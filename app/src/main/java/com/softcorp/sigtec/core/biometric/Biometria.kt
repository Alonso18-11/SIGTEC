package com.softcorp.sigtec.core.biometric

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class DisponibilidadBiometrica {
    HUELLA,                   // sensor con huella registrada
    SOLO_BLOQUEO_PANTALLA,    // sin sensor: PIN, patrón o contraseña del celular
    SIN_HUELLA_REGISTRADA,    // tiene sensor pero ninguna huella
    SIN_SEGURIDAD;            // el celular no tiene ningún bloqueo

    val permiteIngreso: Boolean get() = this == HUELLA || this == SOLO_BLOQUEO_PANTALLA
    val puedeConfirmar: Boolean get() = this != SIN_SEGURIDAD
}

sealed interface ResultadoBiometrico {
    /** instante = fecha y hora de la confirmación que exige el RF-23 */
    data class Exito(val instante: Instant) : ResultadoBiometrico
    data object Cancelado : ResultadoBiometrico
    data class Error(val mensaje: String) : ResultadoBiometrico
}

// Android 11+: huella de seguridad alta + credencial del dispositivo.
// Android 10 o menos: esa combinación no existe; se usa la categoría estándar (sección 16.2, paso 6).
private val biometriaPermitida: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG else BIOMETRIC_WEAK

private val autenticadoresPermitidos: Int
    get() = biometriaPermitida or DEVICE_CREDENTIAL

@Singleton
class EvaluadorBiometrico @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun evaluar(): DisponibilidadBiometrica =
        when (BiometricManager.from(context).canAuthenticate(biometriaPermitida)) {
            BiometricManager.BIOMETRIC_SUCCESS -> DisponibilidadBiometrica.HUELLA
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> DisponibilidadBiometrica.SIN_HUELLA_REGISTRADA
            else ->
                if (context.getSystemService(KeyguardManager::class.java).isDeviceSecure)
                    DisponibilidadBiometrica.SOLO_BLOQUEO_PANTALLA
                else DisponibilidadBiometrica.SIN_SEGURIDAD
        }
}

class VerificadorBiometrico(private val actividad: FragmentActivity) {

    fun verificar(
        titulo: String,
        subtitulo: String? = null,
        alTerminar: (ResultadoBiometrico) -> Unit
    ) {
        val prompt = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    alTerminar(ResultadoBiometrico.Exito(Instant.now()))
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    alTerminar(
                        if (codigo in CODIGOS_CANCELACION) ResultadoBiometrico.Cancelado
                        else ResultadoBiometrico.Error(mensaje.toString())
                    )
                }
                // onAuthenticationFailed (huella no reconocida) no se maneja:
                // el diálogo sigue abierto y Android permite reintentar.
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .apply { if (subtitulo != null) setSubtitle(subtitulo) }
            // Con DEVICE_CREDENTIAL no se define botón negativo: Android ofrece "Usar PIN"
            .setAllowedAuthenticators(autenticadoresPermitidos)
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        val CODIGOS_CANCELACION = setOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED
        )
    }
}

@Composable
fun rememberVerificadorBiometrico(): VerificadorBiometrico {
    // MainActivity hereda de AppCompatActivity (una FragmentActivity): por eso se cambió en la parte 0
    val actividad = LocalActivity.current as FragmentActivity
    return remember(actividad) { VerificadorBiometrico(actividad) }
}

@Suppress("DEPRECATION")
fun intentRegistrarHuella(): Intent = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
        Intent(Settings.ACTION_BIOMETRIC_ENROLL)
            .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Intent(Settings.ACTION_FINGERPRINT_ENROLL)
    else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
}