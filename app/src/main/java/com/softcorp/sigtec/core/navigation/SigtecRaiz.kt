package com.softcorp.sigtec.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SigtecRaiz(vm: RaizViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val usuario by vm.usuarioActual.collectAsStateWithLifecycle()

    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val destinos = usuario?.let { DestinoPrincipal.para(it.perfil) }.orEmpty()
    val mostrarBarra = destinos.any { destinoActual?.hasRoute(it.ruta::class) == true }

    // La navegación sigue a la sesión: entrar lleva a Inicio; salir (o perderla) lleva al login.
    // En ambos casos se limpia la pila, para que "atrás" nunca cruce la frontera de la sesión (HU-01).
    LaunchedEffect(usuario, destinoActual) {
        val destino = destinoActual ?: return@LaunchedEffect
        val enLogin = destino.hasRoute(Ruta.Login::class)
        when {
            usuario != null && enLogin -> navController.navigate(Ruta.Inicio) {
                popUpTo<Ruta.Login> { inclusive = true }
            }
            usuario == null && !enLogin -> navController.navigate(Ruta.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

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
            accesoGraph(alIngresarDemo = vm::entrarDemo, alCerrarSesion = vm::salir)
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