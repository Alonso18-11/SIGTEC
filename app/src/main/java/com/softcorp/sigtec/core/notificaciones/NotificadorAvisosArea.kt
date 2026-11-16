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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-18 (D-8, RF-1819 a RF-1821): los avisos del jefe («Casos demorados», «Resumen del día»), con el mismo mecanismo
 * que las asignaciones (HU-07) pero en su propio canal, para silenciarlos sin perder las asignaciones. Van con la
 * etiqueta del canal, así sus ids no chocan con los de las asignaciones.
 */
@Singleton
class NotificadorAvisosArea @Inject constructor(
    @param:ApplicationContext private val contexto: Context
) {

    fun avisar(id: Int, contenido: ContenidoAviso) {
        if (!tienePermiso()) return   // RF-1823: sin permiso, la tarea corre igual pero sin aviso
        crearCanalSiFalta()

        // RF-1821: sin extras, la app abre en Inicio o vuelve a la pantalla en la que estaba
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val alTocar = PendingIntent.getActivity(
            contexto, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // RF-1820: con el celular bloqueado solo se ve el título, sin números
        val publica = NotificationCompat.Builder(contexto, CANAL_AVISOS_AREA)
            .setSmallIcon(R.drawable.ic_aviso_asignacion)
            .setContentTitle(contenido.textoPublico)
            .build()
        val notificacion = NotificationCompat.Builder(contexto, CANAL_AVISOS_AREA)
            .setSmallIcon(R.drawable.ic_aviso_asignacion)
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido.texto))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publica)
            .setAutoCancel(true)   // RF-1821: se quita al tocarlo
            .setContentIntent(alTocar)
            .build()

        try {
            NotificationManagerCompat.from(contexto).notify(ETIQUETA, id, notificacion)   // reemplaza el anterior
        } catch (e: SecurityException) {
            // El permiso se pudo retirar entre la comprobación y el aviso: no debe romper la tarea
            Log.w(TAG, "No se pudo mostrar el aviso $id", e)
        }
    }

    /** RF-1814: quita el aviso si sigue a la vista. */
    fun quitar(id: Int) {
        NotificationManagerCompat.from(contexto).cancel(ETIQUETA, id)
    }

    private fun tienePermiso(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // RF-1819: canal propio, de importancia normal; crearlo es idempotente
    private fun crearCanalSiFalta() {
        val canal = NotificationChannel(CANAL_AVISOS_AREA, "Avisos del área", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Casos demorados y resumen del día" }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    companion object {
        const val CANAL_AVISOS_AREA = "avisos_area"
        const val ID_CASOS_DEMORADOS = 1
        const val ID_RESUMEN_DIA = 2
        private const val ETIQUETA = "avisos_area"
        private const val TAG = "NotificadorAvisosArea"
    }
}
