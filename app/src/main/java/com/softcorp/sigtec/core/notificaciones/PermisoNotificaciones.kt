package com.softcorp.sigtec.core.notificaciones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Permiso

/**
 * HU-07 (RF-708, RF-710): el permiso solo existe desde Android 13 y solo se le pide a quien recibe
 * asignaciones (el técnico); al jefe y al personal de sistemas no.
 */
fun debePedirPermisoNotificaciones(sdk: Int, concedido: Boolean, perfil: Perfil?): Boolean =
    sdk >= Build.VERSION_CODES.TIRAMISU && !concedido && perfil?.puede(Permiso.ATENDER_INCIDENCIA) == true

/**
 * Pide el permiso una vez por cada apertura de la app (RF-708). Si la persona lo niega no se hace nada
 * más: la app funciona igual, sin avisos (RF-709). Android deja de mostrar el diálogo tras dos rechazos,
 * así que no hace falta recordar nada en DataStore (D-6 del plan).
 */
@Composable
fun PedirPermisoNotificaciones(perfil: Perfil?, habilitado: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return   // RF-710
    val contexto = LocalContext.current
    var pedido by rememberSaveable { mutableStateOf(false) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(perfil, habilitado) {
        val concedido = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (habilitado && !pedido && debePedirPermisoNotificaciones(Build.VERSION.SDK_INT, concedido, perfil)) {
            pedido = true
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
