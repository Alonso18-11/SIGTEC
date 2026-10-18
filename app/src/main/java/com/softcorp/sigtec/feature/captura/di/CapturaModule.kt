package com.softcorp.sigtec.feature.captura.di

import com.softcorp.sigtec.feature.captura.data.CatalogoEquiposRepositoryImpl
import com.softcorp.sigtec.feature.captura.domain.CatalogoEquiposRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CapturaModule {

    @Binds
    @Singleton
    abstract fun bindCatalogoEquiposRepository(impl: CatalogoEquiposRepositoryImpl): CatalogoEquiposRepository
}
