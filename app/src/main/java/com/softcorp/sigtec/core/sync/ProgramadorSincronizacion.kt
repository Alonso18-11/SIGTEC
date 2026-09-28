package com.softcorp.sigtec.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramadorSincronizacion @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun subirCambios() {
        val trabajo = OneTimeWorkRequestBuilder<SubirCambiosWorker>()
            // Sin red, WorkManager espera; aunque la app esté cerrada, sube al recuperar la señal
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // APPEND_OR_REPLACE: si una subida está en curso, la nueva se encola detrás de ella.
        // Con KEEP, un cambio que llega justo cuando la subida termina quedaría sin subir.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
    }

    private companion object {
        const val NOMBRE = "subir-cambios"
    }
}
