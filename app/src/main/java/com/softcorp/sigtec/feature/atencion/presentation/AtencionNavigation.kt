package com.softcorp.sigtec.feature.atencion.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.atencionGraph(navController: NavController) {
    composable<Ruta.MisTareas> {
        MisTareasRoute(
            alAbrir = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) }
        )
    }
    composable<Ruta.SolicitarRepuesto> { entrada ->
        val ruta = entrada.toRoute<Ruta.SolicitarRepuesto>()
        PantallaEnConstruccion(
            titulo = "Solicitar repuesto · ${ruta.incidenciaId}",
            historia = "HU-11",
            acciones = listOf("Enviar con huella" to { navController.popBackStack() })
        )
    }
    composable<Ruta.RegistrarInforme> {
        InformeTecnicoRoute(
            alVolver = { navController.popBackStack() },
            alCerrar = { navController.popBackStack() }
        )
    }
    composable<Ruta.EstadoRepuestos> {
        PantallaEnConstruccion(titulo = "Estado de repuestos", historia = "HU-11 · HU-18")
    }
}