package com.softcorp.sigtec.feature.captura.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Dibuja [contenido] (la cámara) solo con el permiso concedido; si no, explica para qué se usa y lo pide al
 * pulsar «Permitir cámara», nunca antes (RF-916 a RF-920). Lo usan «Escanear etiqueta» y «Evidencia fotográfica».
 */
@Composable
fun ConPermisoCamara(
    textoNegado: String?,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    val contexto = LocalContext.current
    val actividad = LocalActivity.current
    val tieneCamara = remember { contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var concedido by remember { mutableStateOf(camaraConcedida(contexto)) }
    // Solo recuerda una negativa: con el permiso concedido no importa si se pidió
    var yaSePidio by rememberSaveable { mutableStateOf(false) }
    var androidLoExplicaria by remember { mutableStateOf(explicariaElPermiso(actividad)) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        concedido = otorgado
        yaSePidio = !otorgado
        androidLoExplicaria = explicariaElPermiso(actividad)
    }

    Box(modifier) {
        when (val vista = vistaPermiso(tieneCamara, concedido, yaSePidio, androidLoExplicaria)) {
            VistaPermiso.CAMARA -> contenido()
            else -> ExplicacionPermisoCamara(
                vista = vista,
                textoNegado = textoNegado,
                alPermitir = { lanzador.launch(Manifest.permission.CAMERA) },
                alAbrirAjustes = { contexto.startActivity(ajustesDeLaApp(contexto)) } // RF-919
            )
        }
    }
}

private fun camaraConcedida(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Lo que responde `shouldShowRequestPermissionRationale`: verdadero si Android aún mostraría su diálogo. */
private fun explicariaElPermiso(actividad: Activity?): Boolean =
    actividad != null && ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)

/** La página de SIGTEC en los ajustes de Android, donde están sus permisos. */
private fun ajustesDeLaApp(contexto: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
