package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.presentation.claveDictado
import com.softcorp.sigtec.feature.incidencias.presentation.CLAVE_AVISO

// HU-13 (RF-1311): los campos de «Registrar falla» con micrófono
private val CAMPOS_FALLA = listOf(CampoDictado.SINTOMAS, CampoDictado.SOLUCION)

fun NavGraphBuilder.diccionarioGraph(navController: NavController) {
    composable<Ruta.Diccionario> { entrada ->
        // Se retira al leerlo: al volver a esta pantalla no se repite
        val aviso = remember { entrada.savedStateHandle.remove<String>(CLAVE_AVISO) }
        DiccionarioRoute(
            aviso = aviso,
            alRegistrar = { navController.navigate(Ruta.RegistrarFalla()) }
        )
    }
    // El ViewModel lee incidenciaOrigenId de los argumentos de la ruta (RegistrarFallaViewModel.ARG_INCIDENCIA_ORIGEN_ID)
    composable<Ruta.RegistrarFalla> { entrada ->
        val textosDictados = CAMPOS_FALLA.associateWith { campo ->
            entrada.savedStateHandle.getStateFlow<String?>(claveDictado(campo), null)
                .collectAsStateWithLifecycle().value
        }
        RegistrarFallaRoute(
            textosDictados = textosDictados,
            alConsumirDictado = { campo -> entrada.savedStateHandle[claveDictado(campo)] = null },
            alDictar = { campo -> navController.navigate(Ruta.DictarTexto(campo.name, "Registrar falla")) },
            alVolver = { navController.popBackStack() },
            // RF-1313: el diccionario (o el detalle, RF-1317) muestra el aviso al volver
            alGuardada = {
                navController.previousBackStackEntry?.savedStateHandle
                    ?.set(CLAVE_AVISO, "Falla registrada en el diccionario")
                navController.popBackStack()
            }
        )
    }
}
