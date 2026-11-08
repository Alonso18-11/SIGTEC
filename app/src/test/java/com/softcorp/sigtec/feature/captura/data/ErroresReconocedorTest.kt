package com.softcorp.sigtec.feature.captura.data

import android.speech.SpeechRecognizer
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-10 (RF-1015 a RF-1018): cada error de Android termina en el mensaje que corresponde. */
class ErroresReconocedorTest {

    @Test
    fun `sin coincidencias o sin voz es que no se entendio`() {
        assertEquals(ErrorDictado.SIN_ENTENDER, errorDictadoDe(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(ErrorDictado.SIN_ENTENDER, errorDictadoDe(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
    }

    @Test
    fun `los errores de red y de idioma no disponible son sin conexion`() {
        assertEquals(ErrorDictado.SIN_CONEXION, errorDictadoDe(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(ErrorDictado.SIN_CONEXION, errorDictadoDe(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(ErrorDictado.SIN_CONEXION, errorDictadoDe(SpeechRecognizer.ERROR_SERVER))
        assertEquals(ErrorDictado.SIN_CONEXION, errorDictadoDe(11))   // ERROR_SERVER_DISCONNECTED (API 31)
        assertEquals(ErrorDictado.SIN_CONEXION, errorDictadoDe(13))   // ERROR_LANGUAGE_UNAVAILABLE (API 31)
    }

    @Test
    fun `sin permiso vuelve a la explicacion`() {
        assertEquals(ErrorDictado.SIN_PERMISO, errorDictadoDe(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
    }

    @Test
    fun `audio, cliente, ocupado o desconocido son del microfono`() {
        assertEquals(ErrorDictado.MICROFONO, errorDictadoDe(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(ErrorDictado.MICROFONO, errorDictadoDe(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(ErrorDictado.MICROFONO, errorDictadoDe(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(ErrorDictado.MICROFONO, errorDictadoDe(999))
    }
}
