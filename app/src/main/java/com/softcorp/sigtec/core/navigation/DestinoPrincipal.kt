package com.softcorp.sigtec.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Permiso

enum class DestinoPrincipal(
    val ruta: Ruta,
    val etiqueta: String,
    val icono: ImageVector,
    val permiso: Permiso?        // null = visible para todos
) {
    INICIO(Ruta.Inicio, "Inicio", Icons.Outlined.Home, null),
    INCIDENCIAS(Ruta.ListaIncidencias, "Incidencias", Icons.AutoMirrored.Outlined.List, Permiso.CONSULTAR_INCIDENCIAS),
    MIS_TAREAS(Ruta.MisTareas, "Mis tareas", Icons.AutoMirrored.Outlined.Assignment, Permiso.ATENDER_INCIDENCIA),
    DICCIONARIO(Ruta.Diccionario, "Diccionario", Icons.AutoMirrored.Outlined.MenuBook, Permiso.CONSULTAR_DICCIONARIO),
    INDICADORES(Ruta.Indicadores, "Indicadores", Icons.Outlined.BarChart, Permiso.VER_INDICADORES);

    companion object {
        fun para(perfil: Perfil): List<DestinoPrincipal> =
            entries.filter { it.permiso == null || perfil.puede(it.permiso) }
    }
}