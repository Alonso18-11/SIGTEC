package com.softcorp.sigtec.feature.captura.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

/**
 * HU-09 (RF-909, RF-913): «Escanear etiqueta» deja aquí el código confirmado, en el `savedStateHandle` de la
 * pantalla que la abrió. Es la única clave, así la búsqueda del historial (HU-16) lo recibe igual.
 */
const val CLAVE_CODIGO_ESCANEADO = "codigoEscaneado"

fun NavGraphBuilder.capturaGraph(navController: NavController) {
    composable<Ruta.EscanearEtiqueta> {
        EscanearEtiquetaRoute(
            alVolver = { navController.popBackStack() }, // RF-912: sale sin dejar nada
            alUsarCodigo = { codigo ->
                navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_CODIGO_ESCANEADO, codigo)
                navController.popBackStack()
            }
        )
    }
    composable<Ruta.CapturarEvidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.CapturarEvidencia>()
        PantallaEnConstruccion(
            titulo = "Evidencia fotográfica · ${ruta.incidenciaId}",
            historia = "HU-09",
            acciones = listOf("Listo" to { navController.popBackStack() })
        )
    }
    composable<Ruta.DictarTexto> {
        PantallaEnConstruccion(
            titulo = "Dictar",
            historia = "HU-10",
            acciones = listOf("Usar este texto" to { navController.popBackStack() })
        )
    }
}