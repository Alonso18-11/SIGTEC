package com.softcorp.sigtec.core.di

import com.softcorp.sigtec.core.data.PreferenciasSeguridadDataStore
import com.softcorp.sigtec.core.data.SesionRepositoryFirebase
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SesionModule {

    @Binds
    @Singleton
    abstract fun bindSesionRepository(impl: SesionRepositoryFirebase): SesionRepository

    @Binds
    @Singleton
    abstract fun bindPreferenciasSeguridad(
        impl: PreferenciasSeguridadDataStore
    ): PreferenciasSeguridadRepository
}