package com.softcorp.sigtec.feature.atencion.presentation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion
import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.presentation.claveDictado

// HU-10 (D-2): los campos del informe con micrófono
private val CAMPOS_INFORME = listOf(CampoDictado.DIAGNOSTICO, CampoDictado.SOLUCION, CampoDictado.OBSERVACIONES)

fun NavGraphBuilder.atencionGraph(navController: NavController) {
    composable<Ruta.MisTareas> {
        MisTareasRoute(
            alAbrir = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) }
        )
    }
    composable<Ruta.SolicitarRepuesto> { entrada ->
        val ruta = entrada.toRoute<Ruta.SolicitarRepuesto>()
        PantallaEnConstruccion(
            titulo = "Solicitar repuesto · ${ruta.incidenciaId}",
            historia = "HU-11",
            acciones = listOf("Enviar con huella" to { navController.popBackStack() })
        )
    }
    composable<Ruta.RegistrarInforme> { entrada ->
        val id = entrada.toRoute<Ruta.RegistrarInforme>().incidenciaId
        // HU-10 (RF-1010): «Dictar» deja el texto con la clave de su campo
        val textosDictados = CAMPOS_INFORME.associateWith { campo ->
            entrada.savedStateHandle.getStateFlow<String?>(claveDictado(campo), null)
                .collectAsStateWithLifecycle().value
        }
        InformeTecnicoRoute(
            alVolver = { navController.popBackStack() },
            alCerrar = { navController.popBackStack() },
            // RF-938: el informe sigue en la pila con su ViewModel, así que al volver conserva lo escrito
            alAgregarFoto = { navController.navigate(Ruta.CapturarEvidencia(id)) },
            textosDictados = textosDictados,
            alConsumirDictado = { campo -> entrada.savedStateHandle[claveDictado(campo)] = null },
            alDictar = { campo, subtitulo -> navController.navigate(Ruta.DictarTexto(campo.name, subtitulo)) }
        )
    }
    composable<Ruta.EstadoRepuestos> {
        PantallaEnConstruccion(titulo = "Estado de repuestos", historia = "HU-11 · HU-18")
    }
}