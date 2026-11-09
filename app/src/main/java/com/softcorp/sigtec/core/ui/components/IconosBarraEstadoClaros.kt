package com.softcorp.sigtec.core.ui.components

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Pantallas con `CabeceraHero` (Inicio, Detalle): mientras se ven, los íconos de la barra de estado van en blanco, porque
 * la cabecera oscura queda detrás de ellos. Al salir vuelven a lo que pide el tema (oscuros en modo claro).
 */
@Composable
fun IconosBarraEstadoClaros() {
    val vista = LocalView.current
    val temaOscuro = isSystemInDarkTheme()
    if (vista.isInEditMode) return   // en las vistas previas no hay ventana
    DisposableEffect(vista, temaOscuro) {
        val ventana = (vista.context as? Activity)?.window
        val controlador = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        controlador?.isAppearanceLightStatusBars = false
        onDispose { controlador?.isAppearanceLightStatusBars = !temaOscuro }
    }
}
