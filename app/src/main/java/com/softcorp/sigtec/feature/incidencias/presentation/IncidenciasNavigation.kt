package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta

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
        val id = entrada.toRoute<Ruta.DetalleIncidencia>().incidenciaId
        // Se retira al leerlo: al volver a esta pantalla no se repite
        val aviso = remember { entrada.savedStateHandle.remove<String>(CLAVE_AVISO) }
        DetalleIncidenciaRoute(
            aviso = aviso,
            acciones = AccionesDetalle(
                alVolver = { navController.popBackStack() },
                alAsignar = { navController.navigate(Ruta.AsignarIncidencia(id)) },
                alSolicitarRepuesto = { navController.navigate(Ruta.SolicitarRepuesto(id)) },
                alCerrar = { navController.navigate(Ruta.RegistrarInforme(id)) },
                alAgregarFoto = { navController.navigate(Ruta.CapturarEvidencia(id)) },
                alAbrirAsistente = { navController.navigate(Ruta.Asistente(id)) },
                alVerHistorial = { codigo -> navController.navigate(Ruta.HistorialEquipo(codigo)) }
            )
        )
    }
}
