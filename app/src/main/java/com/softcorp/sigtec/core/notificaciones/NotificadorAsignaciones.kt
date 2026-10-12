package com.softcorp.sigtec.core.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.softcorp.sigtec.MainActivity
import com.softcorp.sigtec.R
import com.softcorp.sigtec.core.domain.model.Incidencia
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-07: publica la notificación local «Nueva incidencia asignada». La genera el propio celular al
 * bajar el cambio, sin servidor (plan Spark, D-1). La usarán también la HU-17 y la HU-18 (D-4).
 */
@Singleton
class NotificadorAsignaciones @Inject constructor(
    @ApplicationContext private val contexto: Context
) {

    fun avisar(incidencia: Incidencia, usuarioId: String) {
        if (!tienePermiso()) return   // RF-709: sin permiso, la app funciona igual pero sin avisos
        crearCanalSiFalta()

        val contenido = contenidoAvisoAsignacion(incidencia)
        val id = idNotificacion(incidencia.id)
        // Si la app ya está abierta, el toque llega a onNewIntent en vez de abrir otra actividad
        val intent = Intent(contexto, MainActivity::class.java).apply {
            putExtra(EXTRA_INCIDENCIA_ID, incidencia.id)
            putExtra(EXTRA_USUARIO_ID, usuarioId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val alTocar = PendingIntent.getActivity(
            contexto, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // RF-707: con el celular bloqueado solo se ve el título, sin el equipo ni la descripción
        val publica = NotificationCompat.Builder(contexto, CANAL_ASIGNACIONES)
            .setSmallIcon(R.drawable.ic_aviso_asignacion)
            .setContentTitle(contenido.textoPublico)
            .build()
        val notificacion = NotificationCompat.Builder(contexto, CANAL_ASIGNACIONES)
            .setSmallIcon(R.drawable.ic_aviso_asignacion)
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido.texto))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publica)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)          // RF-711: se quita al tocarla
            .setContentIntent(alTocar)
            .build()

        try {
            NotificationManagerCompat.from(contexto).notify(id, notificacion)   // RF-704: mismo id, reemplaza
        } catch (e: SecurityException) {
            // El permiso se pudo retirar entre la comprobación y el aviso: no debe romper la sincronización
            Log.w(TAG, "No se pudo mostrar el aviso de la incidencia ${incidencia.id}", e)
        }
    }

    private fun tienePermiso(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // RF-706: canal propio que la persona puede silenciar en Android; crearlo es idempotente (D-5)
    private fun crearCanalSiFalta() {
        val canal = NotificationChannel(CANAL_ASIGNACIONES, "Asignaciones", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Incidencias nuevas que te asignan" }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    companion object {
        const val CANAL_ASIGNACIONES = "asignaciones"
        const val EXTRA_INCIDENCIA_ID = "aviso_incidencia_id"
        const val EXTRA_USUARIO_ID = "aviso_usuario_id"
        private const val TAG = "NotificadorAsignaciones"
    }
}
