package com.softcorp.sigtec.core.ui.components

import com.softcorp.sigtec.core.theme.SigtecTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Pantallas con `CabeceraHero` (Inicio, Detalle): mientras se ven, los íconos de la barra de estado van en blanco, porque
 * la cabecera oscura queda detrás de ellos. Al salir vuelven a lo que pide el tema (oscuros en modo claro).
 * Cuenta las pantallas con cabecera que están montadas: en una transición (por ejemplo del Login a Inicio) la que
 * sale se desmonta después de que entra la otra, y sin el conteo dejaría los íconos oscuros sobre la cabecera.
 */
@Composable
fun IconosBarraEstadoClaros() {
    val vista = LocalView.current
    val temaOscuro = isSystemInDarkTheme()
    if (vista.isInEditMode) return   // en las vistas previas no hay ventana
    DisposableEffect(vista, temaOscuro) {
        val ventana = (vista.context as? Activity)?.window
        val controlador = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        pantallasConCabecera++
        controlador?.isAppearanceLightStatusBars = false
        onDispose {
            pantallasConCabecera--
            if (pantallasConCabecera == 0) controlador?.isAppearanceLightStatusBars = !temaOscuro
        }
    }
}

// Solo se toca desde el hilo principal (efectos de Compose)
private var pantallasConCabecera = 0

/**
 * Franja del color de la cabecera detrás de la barra de estado. Va encima del contenido que se desplaza, así los íconos
 * claros siguen visibles cuando la cabecera ya pasó. Con [visible] en false no se dibuja (con la cabecera arriba no hace
 * falta y taparía sus círculos).
 */
@Composable
fun FondoBarraEstado(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(SigtecTheme.marca.hero)
    )
}

