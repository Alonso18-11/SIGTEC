package com.softcorp.sigtec.feature.incidencias.di

import com.softcorp.sigtec.feature.incidencias.data.IncidenciaRepositoryImpl
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncidenciasModule {

    @Binds
    @Singleton
    abstract fun bindIncidenciaRepository(impl: IncidenciaRepositoryImpl): IncidenciaRepository
}
