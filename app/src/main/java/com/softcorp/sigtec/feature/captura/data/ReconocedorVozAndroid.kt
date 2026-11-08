package com.softcorp.sigtec.feature.captura.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.softcorp.sigtec.feature.captura.domain.ErrorDictado
import com.softcorp.sigtec.feature.captura.domain.EventoDictado
import com.softcorp.sigtec.feature.captura.domain.ReconocedorVoz
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-10 (plan 4.2): el reconocedor de voz que trae Android, sin costo (sección 17 del producto). `SpeechRecognizer`
 * exige el hilo principal (P-1), por eso el flujo corre ahí. No graba ni guarda el audio (RF-1013).
 */
@Singleton
class ReconocedorVozAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context
) : ReconocedorVoz {

    // Solo se toca desde el hilo principal: el flujo corre ahí y detener() lo llama la pantalla
    private var enCurso: SpeechRecognizer? = null

    // D-5 y D-6: la primera configuración que el servicio de voz aceptó; las escuchas siguientes empiezan por ella
    private var configuracionAceptada = 0

    private val principal = Handler(Looper.getMainLooper())

    override val disponible: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(contexto)

    override fun escuchar(): Flow<EventoDictado> = callbackFlow {
        if (!disponible) {
            trySend(EventoDictado.Error(ErrorDictado.SIN_SERVICIO))   // RF-1017
            close()
            return@callbackFlow
        }
        var intento = configuracionAceptada
        var reconocedor: SpeechRecognizer? = null
        var terminado = false
        var hablo = false
        var avisoEscuchando = false

        fun empezar() {
            reconocedor?.destroy()
            val actual = SpeechRecognizer.createSpeechRecognizer(contexto)
            reconocedor = actual
            enCurso = actual
            actual.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (!avisoEscuchando) trySend(EventoDictado.Escuchando)   // RF-1004
                    avisoEscuchando = true
                }

                override fun onBeginningOfSpeech() {
                    hablo = true
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    primerResultado(partialResults)?.let {
                        hablo = true
                        trySend(EventoDictado.Parcial(it))                      // RF-1005
                    }
                }

                override fun onResults(results: Bundle?) {
                    configuracionAceptada = intento
                    terminado = true
                    trySend(EventoDictado.Final(primerResultado(results).orEmpty()))   // RF-1007
                    close()
                }

                override fun onError(error: Int) {
                    // El servicio de voz no tiene ese español (o no sin conexión): se prueba el siguiente sin que
                    // la persona lo note, siempre que todavía no haya dicho nada
                    if (error in IDIOMA_NO_DISPONIBLE && !hablo && intento < CONFIGURACIONES.lastIndex) {
                        Log.i(TAG, "El servicio de voz no acepta ${CONFIGURACIONES[intento]} (error $error)")
                        intento++
                        principal.post { if (!terminado) empezar() }
                        return
                    }
                    if (error in IDIOMA_NO_DISPONIBLE) configuracionAceptada = 0
                    Log.w(TAG, "El reconocedor de voz terminó con el error $error")
                    terminado = true
                    trySend(EventoDictado.Error(errorDictadoDe(error)))   // RF-1015 a RF-1018
                    close()
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            actual.startListening(intentDeEscucha(CONFIGURACIONES[intento]))
        }

        empezar()
        // RF-1012 y RF-1014: dejar de recolectar suelta el micrófono
        awaitClose {
            val ultimo = reconocedor
            if (!terminado) ultimo?.cancel()   // si ya terminó, el servicio se desconectó y cancelar solo da un error
            terminado = true
            ultimo?.destroy()
            if (enCurso === ultimo) enCurso = null
        }
    }.flowOn(Dispatchers.Main.immediate)

    override fun detener() {
        enCurso?.stopListening()   // RF-1007: Android entrega lo reconocido en onResults
    }

    private fun intentDeEscucha(configuracion: Configuracion) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, configuracion.idioma)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)                     // RF-1005
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, configuracion.sinConexion)   // D-6
    }

    /** Android ordena los resultados por confianza; se usa el primero (P-3). */
    private fun primerResultado(resultados: Bundle?): String? =
        resultados?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private data class Configuracion(val idioma: String, val sinConexion: Boolean)

    private companion object {
        const val TAG = "ReconocedorVoz"

        // ERROR_LANGUAGE_NOT_SUPPORTED y ERROR_LANGUAGE_UNAVAILABLE (API 31), con su valor por el minSdk 26
        val IDIOMA_NO_DISPONIBLE = setOf(12, 13)

        /**
         * D-5 y D-6: primero español de Perú sin conexión; si el servicio de voz no lo tiene (en un Xiaomi con
         * Android 14 el de Google rechaza `es-PE` sin conexión), los españoles que suele tener, y al final los mismos
         * con conexión.
         */
        val CONFIGURACIONES = listOf(
            Configuracion("es-PE", sinConexion = true),
            Configuracion("es-US", sinConexion = true),
            Configuracion("es-ES", sinConexion = true),
            Configuracion("es-PE", sinConexion = false),
            Configuracion("es-US", sinConexion = false)
        )
    }
}
