package com.softcorp.sigtec.feature.captura.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.capturaGraph(navController: NavController) {
    composable<Ruta.EscanearEtiqueta> {
        PantallaEnConstruccion(
            titulo = "Escanear etiqueta",
            historia = "HU-09",
            acciones = listOf("Usar este código" to { navController.popBackStack() })
        )
    }
    composable<Ruta.CapturarEvidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.CapturarEvidencia>()
        PantallaEnConstruccion(
            titulo = "Evidencia fotográfica · ${ruta.incidenciaId}",
            historia = "HU-09",
            acciones = listOf("Listo" to { navController.popBackStack() })
        )
    }
    composable<Ruta.DictarTexto> {
        PantallaEnConstruccion(
            titulo = "Dictar",
            historia = "HU-10",
            acciones = listOf("Usar este texto" to { navController.popBackStack() })
        )
    }
}