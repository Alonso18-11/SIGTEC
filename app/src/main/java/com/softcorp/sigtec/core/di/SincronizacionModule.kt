package com.softcorp.sigtec.core.di

import com.softcorp.sigtec.core.domain.repository.EstadoDatosRepository
import com.softcorp.sigtec.core.sync.EstadoDatosRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SincronizacionModule {

    @Binds
    @Singleton
    abstract fun bindEstadoDatos(impl: EstadoDatosRepositoryImpl): EstadoDatosRepository
}
