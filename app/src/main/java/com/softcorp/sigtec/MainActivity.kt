package com.softcorp.sigtec

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
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
        // Retema v2: sin el velo blanco que Android pone detrás de la barra de navegación, que se
        // notaba como una franja sobre el fondo teal claro
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
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