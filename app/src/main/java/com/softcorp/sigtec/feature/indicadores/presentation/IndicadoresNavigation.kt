package com.softcorp.sigtec.feature.indicadores.presentation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.feature.captura.presentation.CLAVE_CODIGO_ESCANEADO

fun NavGraphBuilder.indicadoresGraph(navController: NavController) {
    composable<Ruta.Indicadores> {
        // HU-15 (RF-1522): la tarjeta del equipo a evaluar abre su historial
        IndicadoresRoute(alAbrirEquipo = { codigo -> navController.navigate(Ruta.HistorialEquipo(codigo)) })
    }
    // HU-16: el ViewModel lee codigoEquipo de los argumentos de la ruta (HistorialEquipoViewModel.ARG_CODIGO_EQUIPO)
    composable<Ruta.HistorialEquipo> { entrada ->
        // RF-1605: lo deja «Escanear etiqueta» al confirmar, como en el registro (HU-09)
        val codigoEscaneado by entrada.savedStateHandle
            .getStateFlow<String?>(CLAVE_CODIGO_ESCANEADO, null)
            .collectAsStateWithLifecycle()
        HistorialEquipoRoute(
            codigoEscaneado = codigoEscaneado,
            alConsumirCodigo = { entrada.savedStateHandle[CLAVE_CODIGO_ESCANEADO] = null },
            alVolver = { navController.popBackStack() },
            alEscanear = { navController.navigate(Ruta.EscanearEtiqueta) },
            alAbrirIncidencia = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) }   // RF-1620
        )
    }
}
