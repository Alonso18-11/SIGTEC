package com.softcorp.sigtec.feature.atencion.di

import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryImpl
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AtencionModule {

    @Binds
    @Singleton
    abstract fun bindAtencionRepository(impl: AtencionRepositoryImpl): AtencionRepository
}
