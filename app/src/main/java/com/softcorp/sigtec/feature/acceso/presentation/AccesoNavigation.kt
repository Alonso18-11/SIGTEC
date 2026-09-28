package com.softcorp.sigtec.feature.acceso.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.accesoGraph(alCerrarSesion: () -> Unit) {
    composable<Ruta.Login> {
        LoginRoute()
    }
    composable<Ruta.AjustesSeguridad> {
        PantallaEnConstruccion(
            titulo = "Ajustes de seguridad",
            historia = "HU-02",
            acciones = listOf("Cerrar sesión" to alCerrarSesion)
        )
    }
}