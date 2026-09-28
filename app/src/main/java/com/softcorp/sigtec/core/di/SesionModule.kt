package com.softcorp.sigtec.core.di

import com.softcorp.sigtec.core.data.SesionRepositoryFirebase
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SesionModule {

    // HU-01: reemplazar SesionRepositoryDemo por SesionRepositoryFirebase.
    // Es el único cambio necesario; ninguna pantalla ni caso de uso se entera.
    @Binds
    @Singleton
    abstract fun bindSesionRepository(impl: SesionRepositoryFirebase): SesionRepository
}