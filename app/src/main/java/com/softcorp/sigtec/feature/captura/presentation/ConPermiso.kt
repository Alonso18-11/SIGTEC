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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Dibuja [contenido] solo con el [permiso] de Android concedido; si no, explica para qué se usa y lo pide al pulsar
 * el botón de [textos], nunca antes (HU-09: RF-916 a RF-920; HU-10: RF-1019 a RF-1022). [tieneRecurso] es falso si
 * el celular no tiene el hardware (o el servicio) que el permiso protege.
 */
@Composable
fun ConPermiso(
    permiso: String,
    tieneRecurso: Boolean,
    textos: TextosPermiso,
    textoNegado: String?,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    val contexto = LocalContext.current
    val actividad = LocalActivity.current
    var concedido by remember { mutableStateOf(estaConcedido(contexto, permiso)) }
    // Solo recuerda una negativa: con el permiso concedido no importa si se pidió
    var yaSePidio by rememberSaveable { mutableStateOf(false) }
    var androidLoExplicaria by remember { mutableStateOf(explicariaElPermiso(actividad, permiso)) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        concedido = otorgado
        yaSePidio = !otorgado
        androidLoExplicaria = explicariaElPermiso(actividad, permiso)
    }
    // RF-919, RF-922 y RF-1022: el permiso pudo cambiar en los ajustes de Android mientras la app no estaba a la vista
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        concedido = estaConcedido(contexto, permiso)
        androidLoExplicaria = explicariaElPermiso(actividad, permiso)
        if (concedido) yaSePidio = false
    }

    Box(modifier) {
        when (val vista = vistaPermiso(tieneRecurso, concedido, yaSePidio, androidLoExplicaria)) {
            VistaPermiso.CONCEDIDO -> contenido()
            else -> ExplicacionPermiso(
                vista = vista,
                textos = textos,
                textoNegado = textoNegado,
                alPermitir = { lanzador.launch(permiso) },
                alAbrirAjustes = { contexto.startActivity(ajustesDeLaApp(contexto)) } // RF-919, RF-1022
            )
        }
    }
}

/** HU-09: «Escanear etiqueta» y «Evidencia fotográfica» piden la cámara con sus textos de siempre. */
@Composable
fun ConPermisoCamara(
    textoNegado: String?,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    val contexto = LocalContext.current
    val tieneCamara = remember { contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    ConPermiso(
        permiso = Manifest.permission.CAMERA,
        tieneRecurso = tieneCamara,
        textos = TextosPermisoCamara,
        textoNegado = textoNegado,
        modifier = modifier,
        contenido = contenido
    )
}

private fun estaConcedido(contexto: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

/** Lo que responde `shouldShowRequestPermissionRationale`: verdadero si Android aún mostraría su diálogo. */
private fun explicariaElPermiso(actividad: Activity?, permiso: String): Boolean =
    actividad != null && ActivityCompat.shouldShowRequestPermissionRationale(actividad, permiso)

/** La página de SIGTEC en los ajustes de Android, donde están sus permisos. */
private fun ajustesDeLaApp(contexto: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
