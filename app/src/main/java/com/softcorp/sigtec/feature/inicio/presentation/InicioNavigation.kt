package com.softcorp.sigtec.feature.inicio.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.inicioGraph(navController: NavController) {
    composable<Ruta.Inicio> {
        PantallaEnConstruccion(
            titulo = "Inicio",
            historia = "panel del perfil",
            acciones = listOf(
                "Registrar incidencia" to { navController.navigate(Ruta.RegistroIncidencia) },
                "Historial de equipo" to { navController.navigate(Ruta.HistorialEquipo()) },
                "Estado de repuestos" to { navController.navigate(Ruta.EstadoRepuestos) },
                "Ajustes de seguridad" to { navController.navigate(Ruta.AjustesSeguridad) }
            )
        )
    }
}