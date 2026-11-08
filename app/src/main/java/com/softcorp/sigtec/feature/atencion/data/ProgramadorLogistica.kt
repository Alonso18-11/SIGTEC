package com.softcorp.sigtec.feature.atencion.data

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

/**
 * HU-11 (RF-1117, P-3): encola el registro en Logística con la restricción de red, igual que
 * `ProgramadorSincronizacion` encola la subida a Firestore. Si ya hay uno en cola, se queda ese.
 */
@Singleton
class ProgramadorLogistica @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun registrarPendientes() {
        val trabajo = OneTimeWorkRequestBuilder<RegistrarEnLogisticaWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.KEEP, trabajo)
    }

    private companion object {
        const val NOMBRE = "registrar-en-logistica"
    }
}
