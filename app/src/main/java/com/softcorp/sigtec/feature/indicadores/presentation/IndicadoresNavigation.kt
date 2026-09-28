package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.EQUIPO_DEMO
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.indicadoresGraph(navController: NavController) {
    composable<Ruta.Indicadores> {
        PantallaEnConstruccion(
            titulo = "Indicadores del área",
            historia = "HU-15",
            acciones = listOf(
                "Ver equipo crítico $EQUIPO_DEMO" to { navController.navigate(Ruta.HistorialEquipo(EQUIPO_DEMO)) }
            )
        )
    }
    composable<Ruta.HistorialEquipo> { entrada ->
        val ruta = entrada.toRoute<Ruta.HistorialEquipo>()
        PantallaEnConstruccion(
            titulo = "Historial · ${ruta.codigoEquipo ?: "buscar equipo"}",
            historia = "HU-16"
        )
    }
}