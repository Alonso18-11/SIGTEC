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
    val enAtencion: ColorEstado,
    val esperandoRepuesto: ColorEstado,
    val solucionado: ColorEstado,
    val critico: ColorEstado
)

// Valores de "contenido" en modo claro: sección 12.2 del documento
internal val EstadosClaro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFF8B5000), Color(0xFFFFDCBE)),
    enAtencion = ColorEstado(Color(0xFF00677E), Color(0xFFB3EBFF)),
    esperandoRepuesto = ColorEstado(Color(0xFF7A5900), Color(0xFFFFDEA0)),
    solucionado = ColorEstado(Color(0xFF206C2F), Color(0xFFC6EFC8)),
    critico = ColorEstado(Color(0xFFBA1A1A), Color(0xFFFFDAD6))
)

internal val EstadosOscuro = ColoresEstado(
    sinAsignar = ColorEstado(Color(0xFFFFB870), Color(0xFF6A3C00)),
    enAtencion = ColorEstado(Color(0xFF5DD5FC), Color(0xFF004E5F)),
    esperandoRepuesto = ColorEstado(Color(0xFFF0C048), Color(0xFF5C4300)),
    solucionado = ColorEstado(Color(0xFF8DD88F), Color(0xFF005319)),
    critico = ColorEstado(Color(0xFFFFB4AB), Color(0xFF93000A))
)

internal val LocalColoresEstado = staticCompositionLocalOf { EstadosClaro }