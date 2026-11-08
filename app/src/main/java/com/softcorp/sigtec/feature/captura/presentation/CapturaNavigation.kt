package com.softcorp.sigtec.feature.captura.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.feature.captura.domain.CampoDictado

/**
 * HU-09 (RF-909, RF-913): «Escanear etiqueta» deja aquí el código confirmado, en el `savedStateHandle` de la
 * pantalla que la abrió. Es la única clave, así la búsqueda del historial (HU-16) lo recibe igual.
 */
const val CLAVE_CODIGO_ESCANEADO = "codigoEscaneado"

/**
 * HU-10 (RF-1010, D-9): «Dictar» deja el texto en el `savedStateHandle` de la pantalla que la abrió, con una clave
 * por campo, así el informe sabe a cuál de sus campos va cada texto.
 */
fun claveDictado(campo: CampoDictado): String = "textoDictado_${campo.name}"

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
    // El ViewModel lee incidenciaId de los argumentos de la ruta (CapturaEvidenciaViewModel.ARG_INCIDENCIA_ID)
    composable<Ruta.CapturarEvidencia> {
        EvidenciaFotograficaRoute(alVolver = { navController.popBackStack() })   // RF-930: «Listo» o «Volver»
    }
    // El ViewModel lee campo y subtitulo de los argumentos de la ruta (DictadoViewModel.ARG_CAMPO)
    composable<Ruta.DictarTexto> {
        DictarRoute(
            alVolver = { navController.popBackStack() }, // RF-1012: sale sin dejar nada
            alUsarTexto = { campo, texto ->
                navController.previousBackStackEntry?.savedStateHandle?.set(claveDictado(campo), texto)
                navController.popBackStack()
            }
        )
    }
}