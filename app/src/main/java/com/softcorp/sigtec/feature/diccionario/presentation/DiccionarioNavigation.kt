package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.softcorp.sigtec.core.navigation.Ruta
import com.softcorp.sigtec.core.ui.components.PantallaEnConstruccion

fun NavGraphBuilder.diccionarioGraph() {
    composable<Ruta.Diccionario> {
        PantallaEnConstruccion(titulo = "Diccionario de fallas", historia = "HU-13")
    }
    composable<Ruta.Asistente> { entrada ->
        val ruta = entrada.toRoute<Ruta.Asistente>()
        PantallaEnConstruccion(titulo = "Asistente · ${ruta.incidenciaId}", historia = "HU-13 · HU-14")
    }
}