package com.softcorp.sigtec.feature.ia.di

import com.google.firebase.Firebase
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.feature.ia.data.INSTRUCCIONES_BORRADOR
import com.softcorp.sigtec.feature.ia.data.INSTRUCCIONES_CLASIFICACION
import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryImpl
import com.softcorp.sigtec.feature.ia.domain.SugerenciaIARepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * HU-14 (D-6, P-4): el modelo estable de Gemini que no exige facturación. Cambiar de modelo es esta línea; cada
 * sugerencia guarda cuál la generó (RF-1427).
 */
const val MODELO_GEMINI = "gemini-3.5-flash-lite"

// P-3: límites de tiempo de RF-1406 y RF-1423
private const val LIMITE_CLASIFICAR_MS = 15_000L
private const val LIMITE_BORRADOR_MS = 30_000L

// P-2: respuestas cortas y repetibles
private const val TEMPERATURA = 0.2f

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ModeloClasificacion

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ModeloBorrador

/** HU-14: el módulo de la IA (D-10). Gemini por Firebase AI Logic, sin clave en la app (P-1). */
@Module
@InstallIn(SingletonComponent::class)
abstract class IAModule {

    @Binds
    @Singleton
    abstract fun bindSugerenciaIARepository(impl: SugerenciaIARepositoryImpl): SugerenciaIARepository

    companion object {

        // Gemini Developer API (plan Spark): Firebase AI Logic autoriza con su propia cuenta de servicio
        @Provides
        @Singleton
        fun provideFirebaseAI(): FirebaseAI = Firebase.ai(backend = GenerativeBackend.googleAI())

        // RF-1401: el tipo y la prioridad salen como valores cerrados, los mismos de TipoFalla y Prioridad
        @Provides
        @Singleton
        @ModeloClasificacion
        fun provideModeloClasificacion(ai: FirebaseAI): GenerativeModel = ai.generativeModel(
            modelName = MODELO_GEMINI,
            generationConfig = generationConfig {
                temperature = TEMPERATURA
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        "tipo" to Schema.enumeration(TipoFalla.entries.map { it.name }),
                        "prioridad" to Schema.enumeration(Prioridad.entries.map { it.name })
                    )
                )
            },
            systemInstruction = content { text(INSTRUCCIONES_CLASIFICACION) },
            requestOptions = RequestOptions(LIMITE_CLASIFICAR_MS)
        )

        // RF-1417, RF-1419: los cuatro campos del informe; las piezas y las observaciones pueden faltar
        @Provides
        @Singleton
        @ModeloBorrador
        fun provideModeloBorrador(ai: FirebaseAI): GenerativeModel = ai.generativeModel(
            modelName = MODELO_GEMINI,
            generationConfig = generationConfig {
                temperature = TEMPERATURA
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "diagnostico" to Schema.string(),
                        "trabajoRealizado" to Schema.string(),
                        "piezasCambiadas" to Schema.string(),
                        "observaciones" to Schema.string()
                    ),
                    optionalProperties = listOf("piezasCambiadas", "observaciones")
                )
            },
            systemInstruction = content { text(INSTRUCCIONES_BORRADOR) },
            requestOptions = RequestOptions(LIMITE_BORRADOR_MS)
        )
    }
}
