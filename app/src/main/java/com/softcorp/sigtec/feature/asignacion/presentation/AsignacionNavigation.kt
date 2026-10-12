package com.softcorp.sigtec.feature.asignacion.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.feature.incidencias.presentation.CLAVE_AVISO

fun NavGraphBuilder.asignacionGraph(navController: NavController) {
    // HU-06: se llega desde «Asignar técnico» del detalle; el ViewModel lee el id de la ruta
    composable<Ruta.AsignarIncidencia> {
        AsignacionRoute(
            alVolver = { navController.popBackStack() },
            alAsignar = { nombre ->
                // RF-617: el detalle lee y retira este aviso al volver a mostrarse
                navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_AVISO, "Asignada a $nombre")
                navController.popBackStack()
            }
        )
    }
}
