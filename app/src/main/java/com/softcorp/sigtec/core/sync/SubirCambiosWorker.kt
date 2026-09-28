package com.softcorp.sigtec.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Criterio 7 (WorkManager). Adelanta la base de la HU-17.
@HiltWorker
class SubirCambiosWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sincronizador: SincronizadorIncidencias
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Se repite si aparecen cambios nuevos mientras se estaba subiendo
        repeat(MAX_PASADAS) {
            if (!sincronizador.subirPendientes()) return Result.retry()
            if (!sincronizador.hayPendientes()) return Result.success()
        }
        return Result.success()
    }

    private companion object {
        const val MAX_PASADAS = 3
    }
}
