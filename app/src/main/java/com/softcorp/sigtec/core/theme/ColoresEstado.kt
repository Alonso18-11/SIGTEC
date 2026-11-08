package com.softcorp.sigtec.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorEstado(
    val contenido: Color,   // texto e ícono de la etiqueta
    val contenedor: Color,  // fondo de la etiqueta
    val punto: Color = contenido   // punto de 8 dp delante del texto (v2); los avisos armados a mano usan el texto
)

@Immutable
data class ColoresEstado(
    val sinAsignar: ColorEstado,
    val asignada: ColorEstado,
    val enAtencion: ColorEstado,
    val esperandoRepuesto: ColorEstado,
    val solucionado: ColorEstado,
    val critico: ColorEstado,
    val advertencia: ColorEstado   // avisos que no son un estado de incidencia (sin conexión, sin sensor de huella)
)

// DESIGN.md, sección 3. «Esperando repuesto» es violeta para no confundirse con «Sin asignar».
// `advertencia` toma el ámbar claro del aviso de conexión (plan del retema, 2.2).
internal val EstadosClaro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFF6E3D00), Color(0xFFFFE9CC), Color(0xFF8B5000)),
    asignada = ColorEstado(Color(0xFF33474D), Color(0xFFE3ECEF), Color(0xFF4B6269)),
    enAtencion = ColorEstado(Color(0xFF00536A), Color(0xFFD5F1F8), Color(0xFF00677E)),
    esperandoRepuesto = ColorEstado(Color(0xFF4F3585), Color(0xFFEFE6FA), Color(0xFF6B4FA0)),
    solucionado = ColorEstado(Color(0xFF17521F), Color(0xFFDDF3DF), Color(0xFF206C2F)),
    critico = ColorEstado(Color(0xFF8C1212), Color(0xFFFFDAD6), Color(0xFFBA1A1A)),
    advertencia = ColorEstado(Color(0xFF6E3D00), Color(0xFFFFEFD0), Color(0xFF8B5000))
)

// Fondo = punto claro al 25 % sobre la tarjeta oscura; texto = el fondo claro del estado; punto en un
// tono claro para llegar a 3:1 (plan del retema, sección 7).
internal val EstadosOscuro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFFFFE9CC), Color(0xFF342E1D), Color(0xFFF5B25C)),
    asignada = ColorEstado(Color(0xFFE3ECEF), Color(0xFF243338), Color(0xFFA9BBC0)),
    enAtencion = ColorEstado(Color(0xFFD5F1F8), Color(0xFF11343D), Color(0xFF5BC8E3)),
    esperandoRepuesto = ColorEstado(Color(0xFFEFE6FA), Color(0xFF2C2E45), Color(0xFFB9A2E6)),
    solucionado = ColorEstado(Color(0xFFDDF3DF), Color(0xFF193529), Color(0xFF7FD38C)),
    critico = ColorEstado(Color(0xFFFFDAD6), Color(0xFF402124), Color(0xFFFF8A80)),
    advertencia = ColorEstado(Color(0xFFFFEFD0), Color(0xFF342E1D), Color(0xFFF5B25C))
)

internal val LocalColoresEstado = staticCompositionLocalOf { EstadosClaro }
