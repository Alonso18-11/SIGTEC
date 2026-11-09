package com.softcorp.sigtec.feature.diccionario.di

import com.softcorp.sigtec.feature.diccionario.data.DiccionarioRepositoryImpl
import com.softcorp.sigtec.feature.diccionario.domain.DiccionarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiccionarioModule {

    @Binds
    @Singleton
    abstract fun bindDiccionarioRepository(impl: DiccionarioRepositoryImpl): DiccionarioRepository
}
