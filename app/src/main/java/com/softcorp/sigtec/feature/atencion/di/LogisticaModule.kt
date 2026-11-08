package com.softcorp.sigtec.feature.atencion.di

import com.softcorp.sigtec.feature.atencion.data.LogisticaApi
import com.softcorp.sigtec.feature.atencion.data.LogisticaRepositoryImpl
import com.softcorp.sigtec.feature.atencion.domain.LogisticaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** HU-11: Retrofit para la API de Logística (sección 14 del producto). */
@Module
@InstallIn(SingletonComponent::class)
abstract class LogisticaModule {

    @Binds
    @Singleton
    abstract fun bindLogisticaRepository(impl: LogisticaRepositoryImpl): LogisticaRepository

    companion object {
        /**
         * P-1 y D-1: el servidor de prueba en MockAPI.io, con la misma estructura que tendrá la API real. Es la única
         * línea que cambia al pasar al servicio de la empresa; no es una clave.
         */
        const val URL_LOGISTICA = "https://6aefdf69b9925b27ce170b2c.mockapi.io/api/v1/"

        // RF-1108: a los 10 segundos sin respuesta se avisa y se deja enviar igual
        private const val LIMITE_SEGUNDOS = 10L

        @Provides
        @Singleton
        fun provideLogisticaApi(): LogisticaApi {
            val cliente = OkHttpClient.Builder()
                .connectTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .callTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .build()
            // P-2: MockAPI agrega campos propios; SIGTEC lee solo los suyos
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
            return Retrofit.Builder()
                .baseUrl(URL_LOGISTICA)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(LogisticaApi::class.java)
        }
    }
}
