package com.softcorp.sigtec.feature.captura.di

import com.softcorp.sigtec.feature.captura.data.CatalogoEquiposRepositoryImpl
import com.softcorp.sigtec.feature.captura.data.EvidenciaRepositoryImpl
import com.softcorp.sigtec.feature.captura.data.ReconocedorVozAndroid
import com.softcorp.sigtec.feature.captura.domain.CatalogoEquiposRepository
import com.softcorp.sigtec.feature.captura.domain.EvidenciaRepository
import com.softcorp.sigtec.feature.captura.domain.ReconocedorVoz
import com.softcorp.sigtec.feature.captura.data.LinternaAndroid
import com.softcorp.sigtec.feature.captura.domain.Linterna
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

    @Binds
    @Singleton
    abstract fun bindEvidenciaRepository(impl: EvidenciaRepositoryImpl): EvidenciaRepository

    @Binds
    @Singleton
    abstract fun bindReconocedorVoz(impl: ReconocedorVozAndroid): ReconocedorVoz
    // HU-10 (D-8): una sola dueña del flash para toda la app
    @Binds
    @Singleton
    abstract fun bindLinterna(impl: LinternaAndroid): Linterna
}
