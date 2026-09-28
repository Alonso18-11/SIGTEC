package com.softcorp.sigtec.feature.atencion.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.INCIDENCIA_DEMO
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.atencionGraph(navController: NavController) {
    composable<Ruta.MisTareas> {
        PantallaEnConstruccion(
            titulo = "Mis tareas",
            historia = "HU-08",
            acciones = listOf(
                "Atender INC-0124 (demo)" to { navController.navigate(Ruta.DetalleIncidencia(INCIDENCIA_DEMO)) },
                "Repuestos" to { navController.navigate(Ruta.EstadoRepuestos) }
            )
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
    composable<Ruta.RegistrarInforme> { entrada ->
        val ruta = entrada.toRoute<Ruta.RegistrarInforme>()
        PantallaEnConstruccion(
            titulo = "Informe técnico · ${ruta.incidenciaId}",
            historia = "HU-08 · HU-14",
            acciones = listOf(
                "Dictar notas" to { navController.navigate(Ruta.DictarTexto) },
                "Abrir asistente" to { navController.navigate(Ruta.Asistente(ruta.incidenciaId)) },
                "Confirmar cierre con huella" to { navController.popBackStack() }
            )
        )
    }
    composable<Ruta.EstadoRepuestos> {
        PantallaEnConstruccion(titulo = "Estado de repuestos", historia = "HU-11 · HU-18")
    }
}