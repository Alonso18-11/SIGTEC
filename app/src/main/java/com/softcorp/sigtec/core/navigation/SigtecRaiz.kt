package com.softcorp.sigtec.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.feature.acceso.presentation.accesoGraph
import com.softcorp.sigtec.feature.asignacion.presentation.asignacionGraph
import com.softcorp.sigtec.feature.atencion.presentation.atencionGraph
import com.softcorp.sigtec.feature.captura.presentation.capturaGraph
import com.softcorp.sigtec.feature.diccionario.presentation.diccionarioGraph
import com.softcorp.sigtec.feature.incidencias.presentation.incidenciasGraph
import com.softcorp.sigtec.feature.indicadores.presentation.indicadoresGraph
import com.softcorp.sigtec.feature.inicio.presentation.inicioGraph

@Composable
fun SigtecRaiz() {
    val navController = rememberNavController()

    // TEMPORAL hasta la HU-01: el perfil se elige en el login de prueba.
    var perfil by rememberSaveable { mutableStateOf<Perfil?>(null) }

    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val destinos = perfil?.let { DestinoPrincipal.para(it) }.orEmpty()

    // La barra solo se muestra en las pantallas principales, no en los formularios
    val mostrarBarra = destinos.any { destinoActual?.hasRoute(it.ruta::class) == true }

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    destinos.forEach { destino ->
                        NavigationBarItem(
                            selected = destinoActual?.hierarchy?.any { it.hasRoute(destino.ruta::class) } == true,
                            onClick = { navController.navegarAPrincipal(destino.ruta) },
                            icon = { Icon(destino.icono, contentDescription = null) },
                            label = { Text(destino.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Login,
            modifier = Modifier.padding(innerPadding)
        ) {
            accesoGraph(
                alIngresar = { perfilElegido ->
                    perfil = perfilElegido
                    // Se quita el login de la pila: "atrás" no debe volver a él (HU-01)
                    navController.navigate(Ruta.Inicio) {
                        popUpTo<Ruta.Login> { inclusive = true }
                    }
                },
                alCerrarSesion = {
                    perfil = null
                    // Se vacía toda la pila: "atrás" no debe volver a la sesión cerrada (HU-01)
                    navController.navigate(Ruta.Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
            inicioGraph(navController)
            incidenciasGraph(navController)
            asignacionGraph(navController)
            atencionGraph(navController)
            capturaGraph(navController)
            diccionarioGraph()
            indicadoresGraph(navController)
        }
    }
}

// Cambiar de pestaña sin apilar pantallas y conservando el estado de cada una
private fun NavHostController.navegarAPrincipal(ruta: Ruta) = navigate(ruta) {
    popUpTo<Ruta.Inicio> { saveState = true }
    launchSingleTop = true
    restoreState = true
}