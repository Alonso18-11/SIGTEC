package com.softcorp.sigtec.feature.captura.presentation

import com.softcorp.sigtec.feature.captura.domain.CampoDictado
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado

/** HU-10 (RF-1015 a RF-1018): el texto de cada error al dictar, tal como lo dice la spec. */
fun mensajeDictado(error: ErrorDictado): String = when (error) {
    ErrorDictado.SIN_ENTENDER -> "No se entendió. Toca el micrófono e inténtalo de nuevo."
    ErrorDictado.SIN_CONEXION ->
        "Sin conexión, este celular no puede reconocer la voz. Conéctate o escribe el texto en el formulario."
    ErrorDictado.SIN_SERVICIO -> "Este celular no tiene reconocimiento de voz. Escribe el texto en el formulario."
    ErrorDictado.SIN_PERMISO ->
        "Sin el permiso de micrófono no se puede dictar. Puedes escribir el texto en el formulario."
    ErrorDictado.MICROFONO -> "No se pudo usar el micrófono. Inténtalo de nuevo."
}

/** HU-10 (RF-1003): el título de «Dictar» según el campo que la abrió. */
fun tituloDictado(campo: CampoDictado): String = when (campo) {
    CampoDictado.DESCRIPCION -> "Dictar descripción"
    CampoDictado.DIAGNOSTICO -> "Dictar diagnóstico"
    CampoDictado.SOLUCION -> "Dictar solución aplicada"
    CampoDictado.OBSERVACIONES -> "Dictar observaciones"
}
