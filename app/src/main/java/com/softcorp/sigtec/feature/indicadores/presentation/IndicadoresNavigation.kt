package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.indicadoresGraph(navController: NavController) {
    composable<Ruta.Indicadores> {
        // HU-15 (RF-1522): la tarjeta del equipo a evaluar abre su historial
        IndicadoresRoute(alAbrirEquipo = { codigo -> navController.navigate(Ruta.HistorialEquipo(codigo)) })
    }
    composable<Ruta.HistorialEquipo> { entrada ->
        val ruta = entrada.toRoute<Ruta.HistorialEquipo>()
        PantallaEnConstruccion(
            titulo = "Historial - ${ruta.codigoEquipo ?: "buscar equipo"}",
            historia = "HU-16"
        )
    }
}
