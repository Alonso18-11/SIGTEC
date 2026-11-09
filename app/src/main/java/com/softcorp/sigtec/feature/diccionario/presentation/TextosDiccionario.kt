package com.softcorp.sigtec.feature.diccionario.presentation

import androidx.compose.runtime.Composable
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.theme.ColorEstado
import com.softcorp.sigtec.core.theme.SigtecTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** RF-1301: «1 falla registrada», «4 fallas registradas». */
fun cantidadFallas(total: Int): String = if (total == 1) "1 falla registrada" else "$total fallas registradas"

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** RF-1303: la fecha de registro, como el resto de la app («09/11/2026»). */
fun fechaRegistro(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    FORMATO_FECHA.format(instante.atZone(zona))

/**
 * Prototipo 14: cada tipo con su color (Hardware teal, Red ámbar, Software morado). Se reutilizan los colores de los
 * estados porque ya cumplen el contraste en claro y oscuro; la etiqueta siempre lleva el texto.
 */
@Composable
fun colorTipo(tipo: TipoFalla): ColorEstado = when (tipo) {
    TipoFalla.HARDWARE -> SigtecTheme.estados.enAtencion
    TipoFalla.RED -> SigtecTheme.estados.sinAsignar
    TipoFalla.SOFTWARE -> SigtecTheme.estados.esperandoRepuesto
}
