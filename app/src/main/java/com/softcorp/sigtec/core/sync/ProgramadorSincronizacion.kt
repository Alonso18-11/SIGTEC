package com.softcorp.sigtec.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramadorSincronizacion @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun subirCambios() {
        val trabajo = OneTimeWorkRequestBuilder<SubirCambiosWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
    }

    /**
     * HU-17 (RF-1701, D-3): una sola revisión periódica por celular, cada 15 minutos (el mínimo de WorkManager) y solo
     * con conexión. `KEEP` conserva la que ya está programada, así llamarla de nuevo no reinicia su período.
     */
    fun programarRevision() {
        val trabajo = PeriodicWorkRequestBuilder<RevisarAsignacionesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(NOMBRE_REVISION, ExistingPeriodicWorkPolicy.KEEP, trabajo)
    }

    /** HU-17 (RF-1702): al cerrar sesión ya no hay a quién avisar. */
    fun cancelarRevision() {
        WorkManager.getInstance(context).cancelUniqueWork(NOMBRE_REVISION)
    }

    private companion object {
        const val NOMBRE = "subir-cambios"
        const val NOMBRE_REVISION = "revisar-asignaciones"
    }
}
