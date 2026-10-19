package com.softcorp.sigtec.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

@HiltWorker
class SubirCambiosWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sincronizador: SincronizadorIncidencias,
    private val monitor: MonitorConexion
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!debeIntentarSubir(monitor.hayConexionAhora())) return Result.retry()

        repeat(MAX_PASADAS) {
            if (!sincronizador.subirPendientes(Instant.now())) return Result.retry()
            if (!sincronizador.hayPendientes()) return Result.success()
        }
        return Result.success()
    }

    private companion object {
        const val MAX_PASADAS = 3
    }
}
