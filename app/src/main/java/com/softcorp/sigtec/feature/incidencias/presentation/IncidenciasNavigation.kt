package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.EQUIPO_DEMO
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

// Mensaje que el registro deja al detalle recién abierto
private const val CLAVE_AVISO = "aviso"

fun NavGraphBuilder.incidenciasGraph(navController: NavController) {
    composable<Ruta.ListaIncidencias> {
        ListaIncidenciasRoute(
            alAbrir = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) },
            alRegistrar = { navController.navigate(Ruta.RegistroIncidencia) }
        )
    }
    composable<Ruta.RegistroIncidencia> {
        RegistroIncidenciaRoute(
            alVolver = { navController.popBackStack() },
            alEscanear = { navController.navigate(Ruta.EscanearEtiqueta) },
            alDictar = { navController.navigate(Ruta.DictarTexto) },
            alRegistrarse = { id ->
                // Al volver desde el detalle se llega a la lista, no al formulario ya guardado
                navController.navigate(Ruta.DetalleIncidencia(id)) {
                    popUpTo<Ruta.RegistroIncidencia> { inclusive = true }
                }
                navController.currentBackStackEntry?.savedStateHandle
                    ?.set(CLAVE_AVISO, "Incidencia registrada · $POR_SINCRONIZAR")
            }
        )
    }
    composable<Ruta.DetalleIncidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.DetalleIncidencia>()
        val avisos = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            entrada.savedStateHandle.remove<String>(CLAVE_AVISO)?.let { avisos.showSnackbar(it) }
        }
        // TEMPORAL hasta la HU-04: solo el aviso del registro es real
        Box(Modifier.fillMaxSize()) {
            PantallaEnConstruccion(
                titulo = "Detalle de ${ruta.incidenciaId}",
                historia = "HU-04",
                acciones = listOf(
                    "Asignar técnico" to { navController.navigate(Ruta.AsignarIncidencia(ruta.incidenciaId)) },
                    "Solicitar repuesto" to { navController.navigate(Ruta.SolicitarRepuesto(ruta.incidenciaId)) },
                    "Cerrar con informe" to { navController.navigate(Ruta.RegistrarInforme(ruta.incidenciaId)) },
                    "Agregar foto" to { navController.navigate(Ruta.CapturarEvidencia(ruta.incidenciaId)) },
                    "Abrir asistente" to { navController.navigate(Ruta.Asistente(ruta.incidenciaId)) },
                    "Ver historial del equipo" to { navController.navigate(Ruta.HistorialEquipo(EQUIPO_DEMO)) }
                )
            )
            SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).padding(Espaciado.m))
        }
    }
}
