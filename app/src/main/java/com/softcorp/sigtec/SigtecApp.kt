package com.softcorp.sigtec

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.softcorp.sigtec.core.sync.SincronizacionTiempoReal
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SigtecApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sincronizacion: SincronizacionTiempoReal

    override fun onCreate() {
        super.onCreate()          // Hilt inyecta los campos aquí
        sincronizacion.iniciar()  // HU-05: baja y sube cambios mientras haya sesión
    }

    // Permite que los Workers reciban dependencias por inyección (SubirCambiosWorker)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
