package com.softcorp.sigtec.feature.acceso.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta

fun NavGraphBuilder.accesoGraph(navController: NavController) {
    composable<Ruta.Login> { LoginRoute() }
    composable<Ruta.AjustesSeguridad> {
        AjustesSeguridadRoute(alVolver = { navController.popBackStack() })
    }
}