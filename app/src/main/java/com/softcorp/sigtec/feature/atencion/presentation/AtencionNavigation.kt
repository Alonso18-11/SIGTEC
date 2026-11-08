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
import com.softcorp.sigtec.feature.incidencias.presentation.CLAVE_AVISO

// HU-10 (D-2): los campos del informe con micrófono
private val CAMPOS_INFORME = listOf(CampoDictado.DIAGNOSTICO, CampoDictado.SOLUCION, CampoDictado.OBSERVACIONES)

fun NavGraphBuilder.atencionGraph(navController: NavController) {
    composable<Ruta.MisTareas> {
        MisTareasRoute(
            alAbrir = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) }
        )
    }
    // El ViewModel lee incidenciaId de los argumentos de la ruta (SolicitudRepuestoViewModel.ARG_INCIDENCIA_ID)
    composable<Ruta.SolicitarRepuesto> {
        SolicitudRepuestoRoute(
            alVolver = { navController.popBackStack() },
            // RF-1114: el detalle muestra el aviso al volver, como el registro (HU-03)
            alRegistrada = {
                navController.previousBackStackEntry?.savedStateHandle
                    ?.set(CLAVE_AVISO, "Solicitud registrada · Por enviar a Logística")
                navController.popBackStack()
            }
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