package com.softcorp.sigtec.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.softcorp.sigtec.core.biometric.DisponibilidadBiometrica
import com.softcorp.sigtec.core.biometric.ResultadoBiometrico
import com.softcorp.sigtec.core.biometric.rememberVerificadorBiometrico
import com.softcorp.sigtec.core.domain.model.EstadoAcceso
import com.softcorp.sigtec.feature.acceso.presentation.OfertaHuellaDialogo
import com.softcorp.sigtec.feature.acceso.presentation.accesoGraph
import com.softcorp.sigtec.feature.asignacion.presentation.asignacionGraph
import com.softcorp.sigtec.feature.atencion.presentation.atencionGraph
import com.softcorp.sigtec.feature.captura.presentation.capturaGraph
import com.softcorp.sigtec.feature.diccionario.presentation.diccionarioGraph
import com.softcorp.sigtec.feature.incidencias.presentation.incidenciasGraph
import com.softcorp.sigtec.feature.indicadores.presentation.indicadoresGraph
import com.softcorp.sigtec.feature.inicio.presentation.inicioGraph

@Composable
fun SigtecRaiz(vm: RaizViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    val estado by vm.estadoAcceso.collectAsStateWithLifecycle()
    val mostrarOferta by vm.mostrarOfertaHuella.collectAsStateWithLifecycle()
    val verificador = rememberVerificadorBiometrico()

    // Solo hay usuario para la app cuando la sesión está activa (no bloqueada)
    val usuario = (estado as? EstadoAcceso.Activa)?.usuario
    val activa = usuario != null

    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val destinos = usuario?.let { DestinoPrincipal.para(it.perfil) }.orEmpty()

    // La barra solo se muestra en las pantallas principales, no en los formularios
    val mostrarBarra = destinos.any { destinoActual?.hasRoute(it.ruta::class) == true }

    // La navegación sigue al estado de acceso: activa lleva a Inicio; sin sesión o bloqueada, al login.
    // En ambos casos se limpia la pila, para que "atrás" nunca cruce esa frontera (HU-01).
    LaunchedEffect(activa, destinoActual) {
        val destino = destinoActual ?: return@LaunchedEffect
        val enLogin = destino.hasRoute(Ruta.Login::class)
        when {
            activa && enLogin -> navController.navigate(Ruta.Inicio) {
                popUpTo<Ruta.Login> { inclusive = true }
            }
            !activa && !enLogin -> navController.navigate(Ruta.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Oferta de huella después del primer ingreso con contraseña (HU-02, sección 16.2 paso 3)
    if (mostrarOferta) {
        OfertaHuellaDialogo(
            conSensor = vm.disponibilidad == DisponibilidadBiometrica.HUELLA,
            alActivar = {
                verificador.verificar(titulo = "Activa el ingreso con huella") { resultado ->
                    if (resultado is ResultadoBiometrico.Exito) vm.responderOferta(activar = true)
                }
            },
            alRechazar = { vm.responderOferta(activar = false) }
        )
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
            accesoGraph(navController)
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