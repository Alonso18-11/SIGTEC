package com.softcorp.sigtec

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.softcorp.sigtec.core.navigation.AvisoTocado
import com.softcorp.sigtec.core.navigation.RaizViewModel
import com.softcorp.sigtec.core.navigation.SigtecRaiz
import com.softcorp.sigtec.core.notificaciones.NotificadorAsignaciones
import com.softcorp.sigtec.core.theme.SigtecTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // El mismo RaizViewModel que usa SigtecRaiz: ambos toman el de esta actividad
    private val raiz: RaizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // HU-07 (RF-711): al restaurarse (por ejemplo, tras rotar) el aviso ya está en el ViewModel
        if (savedInstanceState == null) leerAviso(intent)
        enableEdgeToEdge()
        setContent {
            SigtecTheme {
                SigtecRaiz()
            }
        }
    }

    // Con la app abierta, tocar el aviso llega aquí (FLAG_ACTIVITY_SINGLE_TOP)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerAviso(intent)
    }

    private fun leerAviso(intent: Intent?) {
        val incidenciaId = intent?.getStringExtra(NotificadorAsignaciones.EXTRA_INCIDENCIA_ID) ?: return
        val usuarioId = intent.getStringExtra(NotificadorAsignaciones.EXTRA_USUARIO_ID) ?: return
        raiz.alTocarAviso(AvisoTocado(incidenciaId, usuarioId))
    }
}