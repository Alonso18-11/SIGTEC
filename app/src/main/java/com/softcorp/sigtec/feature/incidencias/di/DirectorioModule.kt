package com.softcorp.sigtec.feature.incidencias.di

import com.softcorp.sigtec.feature.incidencias.data.DirectorioApi
import com.softcorp.sigtec.feature.incidencias.data.DirectorioRepositoryImpl
import com.softcorp.sigtec.feature.incidencias.domain.DirectorioRepository
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

/**
 * HU-12: Retrofit para el directorio de colaboradores (sección 14 del producto). Tiene su propio Retrofit, aparte del
 * de Logística, para que cada API apunte a su servidor sin tocar `core/di` (plan P-3).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DirectorioModule {

    @Binds
    @Singleton
    abstract fun bindDirectorioRepository(impl: DirectorioRepositoryImpl): DirectorioRepository

    companion object {
        /**
         * D-1: el servidor de prueba en MockAPI.io (proyecto «sigtec-directorio», aparte del de Logística por el límite
         * del plan gratuito). Es la única línea que cambia al pasar al servicio de la empresa; no es una clave.
         */
        const val URL_DIRECTORIO = "https://6af0a81711a6aa866e7fa1fc.mockapi.io/api/v1/"

        // RF-1212: a los 10 segundos sin respuesta se avisa y se escribe a mano
        private const val LIMITE_SEGUNDOS = 10L

        @Provides
        @Singleton
        fun provideDirectorioApi(): DirectorioApi {
            val cliente = OkHttpClient.Builder()
                .connectTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .readTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .callTimeout(LIMITE_SEGUNDOS, TimeUnit.SECONDS)
                .build()
            // MockAPI agrega campos propios (como createdAt); SIGTEC lee solo los suyos
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
            return Retrofit.Builder()
                .baseUrl(URL_DIRECTORIO)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(DirectorioApi::class.java)
        }
    }
}
