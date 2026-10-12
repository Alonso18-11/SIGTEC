package com.softcorp.sigtec.feature.asignacion.di

import com.softcorp.sigtec.feature.asignacion.data.AsignacionRepositoryImpl
import com.softcorp.sigtec.feature.asignacion.domain.AsignacionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AsignacionModule {

    @Binds
    @Singleton
    abstract fun bindAsignacionRepository(impl: AsignacionRepositoryImpl): AsignacionRepository
}
