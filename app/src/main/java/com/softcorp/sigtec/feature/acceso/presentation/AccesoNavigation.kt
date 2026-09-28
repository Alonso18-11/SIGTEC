package com.softcorp.sigtec.feature.acceso.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.accesoGraph(
    alIngresarDemo: (correo: String) -> Unit,
    alCerrarSesion: () -> Unit
) {
    composable<Ruta.Login> {
        PantallaEnConstruccion(
            titulo = "Inicio de sesión",
            historia = "HU-01 · HU-02",
            acciones = listOf(
                "Carlos Mendoza · jefe" to { alIngresarDemo("cmendoza@softcorp.pe") },
                "Luis Ramírez · técnico" to { alIngresarDemo("lramirez@softcorp.pe") },
                "Pedro Cárdenas · personal de sistemas" to { alIngresarDemo("pcardenas@softcorp.pe") }
            )
        )
    }
    composable<Ruta.AjustesSeguridad> {
        PantallaEnConstruccion(
            titulo = "Ajustes de seguridad",
            historia = "HU-02",
            acciones = listOf("Cerrar sesión" to alCerrarSesion)
        )
    }
}