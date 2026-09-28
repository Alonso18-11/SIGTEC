package com.softcorp.sigtec.feature.acceso.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.accesoGraph(
    alIngresar: (Perfil) -> Unit,
    alCerrarSesion: () -> Unit
) {
    composable<Ruta.Login> {
        PantallaEnConstruccion(
            titulo = "Inicio de sesión",
            historia = "HU-01 · HU-02",
            acciones = listOf(
                "Entrar como jefe" to { alIngresar(Perfil.JEFE) },
                "Entrar como técnico" to { alIngresar(Perfil.TECNICO) },
                "Entrar como personal de sistemas" to { alIngresar(Perfil.PERSONAL_SISTEMAS) }
            )
        )
    }
    composable<Ruta.AjustesSeguridad> {
        PantallaEnConstruccion(
            titulo = "Ajustes de seguridad",
            historia = "HU-02",
            acciones = listOf("Cerrar sesión" to alCerrarSesion)
        )
    }
}