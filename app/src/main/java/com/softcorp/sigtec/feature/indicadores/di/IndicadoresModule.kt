package com.softcorp.sigtec.feature.indicadores.di

import com.softcorp.sigtec.feature.indicadores.data.IndicadoresRepositoryImpl
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IndicadoresModule {

    @Binds
    @Singleton
    abstract fun bindIndicadoresRepository(impl: IndicadoresRepositoryImpl): IndicadoresRepository
}
