package com.softcorp.sigtec.feature.ia.di

import com.softcorp.sigtec.feature.ia.data.SugerenciaIARepositoryImpl
import com.softcorp.sigtec.feature.ia.domain.SugerenciaIARepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** HU-14: el módulo de la IA (D-10). El asistente con Gemini se suma en el bloque C. */
@Module
@InstallIn(SingletonComponent::class)
abstract class IAModule {

    @Binds
    @Singleton
    abstract fun bindSugerenciaIARepository(impl: SugerenciaIARepositoryImpl): SugerenciaIARepository
}
