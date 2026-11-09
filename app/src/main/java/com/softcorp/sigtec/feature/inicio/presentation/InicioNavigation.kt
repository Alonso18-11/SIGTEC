package com.softcorp.sigtec.feature.inicio.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta

fun NavGraphBuilder.inicioGraph(navController: NavController) {
    composable<Ruta.Inicio> {
        InicioRoute(
            alAbrirAjustes = { navController.navigate(Ruta.AjustesSeguridad) },
            alAbrirMisTareas = { navController.comoPestana(Ruta.MisTareas) },
            alAbrirLista = { navController.comoPestana(Ruta.ListaIncidencias) },
            alAbrirDetalle = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) },
            alRegistrar = { navController.navigate(Ruta.RegistroIncidencia) },
            // RF-I9: la cámara se abre encima de «Nueva incidencia», así el código leído vuelve al formulario
            alEscanear = {
                navController.navigate(Ruta.RegistroIncidencia)
                navController.navigate(Ruta.EscanearEtiqueta)
            },
            alAbrirRepuestos = { navController.navigate(Ruta.EstadoRepuestos) }
        )
    }
}

/** Igual que el menú inferior: «Mis tareas» y la lista son pestañas, no pantallas que se apilan sobre Inicio. */
private fun NavController.comoPestana(ruta: Ruta) = navigate(ruta) {
    popUpTo<Ruta.Inicio> { saveState = true }
    launchSingleTop = true
    restoreState = true
}
