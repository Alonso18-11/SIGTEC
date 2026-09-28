package com.softcorp.sigtec.feature.incidencias.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.EQUIPO_DEMO
import com.softcorp.sigtec.core.navigation.INCIDENCIA_DEMO
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.incidenciasGraph(navController: NavController) {
    composable<Ruta.ListaIncidencias> {
        PantallaEnConstruccion(
            titulo = "Incidencias",
            historia = "HU-04",
            acciones = listOf(
                "Ver INC-0124 (demo)" to { navController.navigate(Ruta.DetalleIncidencia(INCIDENCIA_DEMO)) },
                "Registrar" to { navController.navigate(Ruta.RegistroIncidencia) }
            )
        )
    }
    composable<Ruta.RegistroIncidencia> {
        PantallaEnConstruccion(
            titulo = "Nueva incidencia",
            historia = "HU-03 · HU-12",
            acciones = listOf(
                "Escanear etiqueta" to { navController.navigate(Ruta.EscanearEtiqueta) },
                "Dictar descripción" to { navController.navigate(Ruta.DictarTexto) },
                "Guardar incidencia" to { navController.popBackStack() }
            )
        )
    }
    composable<Ruta.DetalleIncidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.DetalleIncidencia>()
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
    }
}