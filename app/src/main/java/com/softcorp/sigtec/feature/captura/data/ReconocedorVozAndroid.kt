package com.softcorp.sigtec.feature.captura.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

    override val disponible: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(contexto)

    override fun escuchar(): Flow<EventoDictado> = callbackFlow {
        if (!disponible) {
            trySend(EventoDictado.Error(ErrorDictado.SIN_SERVICIO))   // RF-1017
            close()
            return@callbackFlow
        }
        val reconocedor = SpeechRecognizer.createSpeechRecognizer(contexto)
        enCurso = reconocedor
        reconocedor.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(EventoDictado.Escuchando)                             // RF-1004
            }

            override fun onPartialResults(partialResults: Bundle?) {
                primerResultado(partialResults)?.let { trySend(EventoDictado.Parcial(it)) }   // RF-1005
            }

            override fun onResults(results: Bundle?) {
                trySend(EventoDictado.Final(primerResultado(results).orEmpty()))   // RF-1007
                close()
            }

            override fun onError(error: Int) {
                trySend(EventoDictado.Error(errorDictadoDe(error)))              // RF-1015 a RF-1018
                close()
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        reconocedor.startListening(intentDeEscucha())
        // RF-1012 y RF-1014: dejar de recolectar suelta el micrófono
        awaitClose {
            reconocedor.cancel()
            reconocedor.destroy()
            if (enCurso === reconocedor) enCurso = null
        }
    }.flowOn(Dispatchers.Main.immediate)

    override fun detener() {
        enCurso?.stopListening()   // RF-1007: Android entrega lo reconocido en onResults
    }

    private fun intentDeEscucha() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, IDIOMA)          // D-5: si no lo tiene, Android usa otro español
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)     // RF-1005
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)      // D-6
    }

    /** Android ordena los resultados por confianza; se usa el primero (P-3). */
    private fun primerResultado(resultados: Bundle?): String? =
        resultados?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private companion object {
        const val IDIOMA = "es-PE"
    }
}
