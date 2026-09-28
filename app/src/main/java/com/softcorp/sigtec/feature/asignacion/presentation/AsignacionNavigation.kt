package com.softcorp.sigtec.feature.asignacion.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.asignacionGraph(navController: NavController) {
    composable<Ruta.AsignarIncidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.AsignarIncidencia>()
        PantallaEnConstruccion(
            titulo = "Asignar ${ruta.incidenciaId}",
            historia = "HU-06",
            acciones = listOf("Asignar a Luis Ramírez" to { navController.popBackStack() })
        )
    }
}