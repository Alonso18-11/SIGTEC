package com.softcorp.sigtec.feature.atencion.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.softcorp.sigtec.feature.atencion.domain.RegistrarSolicitudesEnLogistica
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

/**
 * HU-11 (RF-1117, P-3): registra en Logística las solicitudes pendientes, aunque la app esté cerrada. Solo corre
 * con conexión; si Logística no responde, WorkManager lo reintenta con espera creciente.
 */
@HiltWorker
class RegistrarEnLogisticaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val registrar: RegistrarSolicitudesEnLogistica
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val resultado = registrar(Instant.now())
        return if (resultado.pendientes > 0) Result.retry() else Result.success()
    }
}
