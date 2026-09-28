package com.softcorp.sigtec.feature.inicio.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

fun NavGraphBuilder.inicioGraph(navController: NavController) {
    composable<Ruta.Inicio> {
        val vm: InicioViewModel = hiltViewModel()
        val usuario by vm.usuario.collectAsStateWithLifecycle()
        val primerNombre = usuario?.nombre?.substringBefore(' ').orEmpty()

        PantallaEnConstruccion(
            titulo = "Hola, $primerNombre · ${usuario?.cargo.orEmpty()}",
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