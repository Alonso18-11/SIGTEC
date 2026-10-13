package com.softcorp.sigtec.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorEstado(
    val contenido: Color,   // texto e ícono de la etiqueta
    val contenedor: Color   // fondo de la etiqueta
)

@Immutable
data class ColoresEstado(
    val sinAsignar: ColorEstado,
    val asignada: ColorEstado,
    val enAtencion: ColorEstado,
    val esperandoRepuesto: ColorEstado,
    val solucionado: ColorEstado,
    val critico: ColorEstado,
    val advertencia: ColorEstado   // avisos que no son un estado de incidencia (por ejemplo, sin sensor de huella)
)

// Colores de los "lozenges" de Jira: gris = por hacer, azul = en curso, verde = hecho,
// rojo = crítico, amarillo = falta asignar y morado = en espera de un tercero (el repuesto).
internal val EstadosClaro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFF7F5F01), Color(0xFFF8E6A0)),
    asignada = ColorEstado(Color(0xFF44546F), Color(0xFFDCDFE4)),
    enAtencion = ColorEstado(Color(0xFF0055CC), Color(0xFFCCE0FF)),
    esperandoRepuesto = ColorEstado(Color(0xFF5E4DB2), Color(0xFFDFD8FD)),
    solucionado = ColorEstado(Color(0xFF216E4E), Color(0xFFBAF3DB)),
    critico = ColorEstado(Color(0xFFAE2E24), Color(0xFFFFD5D2)),
    advertencia = ColorEstado(Color(0xFF7F5F01), Color(0xFFFFF7D6))
)

internal val EstadosOscuro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFFF5CD47), Color(0xFF533F04)),
    asignada = ColorEstado(Color(0xFFB6C2CF), Color(0xFF38414A)),
    enAtencion = ColorEstado(Color(0xFF85B8FF), Color(0xFF09326C)),
    esperandoRepuesto = ColorEstado(Color(0xFFB8ACF6), Color(0xFF352C63)),
    solucionado = ColorEstado(Color(0xFF7EE2B8), Color(0xFF164B35)),
    critico = ColorEstado(Color(0xFFFD9891), Color(0xFF5D1F1A)),
    advertencia = ColorEstado(Color(0xFFF5CD47), Color(0xFF332E1B))
)

internal val LocalColoresEstado = staticCompositionLocalOf { EstadosClaro }
