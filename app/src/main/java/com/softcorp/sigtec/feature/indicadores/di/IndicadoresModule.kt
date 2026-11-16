package com.softcorp.sigtec.feature.indicadores.di

import com.softcorp.sigtec.core.sync.TareaDiaria
import com.softcorp.sigtec.feature.indicadores.data.HistorialRepositoryImpl
import com.softcorp.sigtec.feature.indicadores.data.IndicadoresRepositoryImpl
import com.softcorp.sigtec.feature.indicadores.data.TareaCasosDemorados
import com.softcorp.sigtec.feature.indicadores.data.TareaResumenDia
import com.softcorp.sigtec.feature.indicadores.domain.HistorialRepository
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IndicadoresModule {

    @Binds
    @Singleton
    abstract fun bindIndicadoresRepository(impl: IndicadoresRepositoryImpl): IndicadoresRepository

    // HU-16: el historial del equipo vive en el mismo módulo que el tablero (D-6)
    @Binds
    @Singleton
    abstract fun bindHistorialRepository(impl: HistorialRepositoryImpl): HistorialRepository

    // HU-18 (D-10): la revisión periódica corre los avisos del jefe
    @Binds
    @IntoSet
    abstract fun bindTareaCasosDemorados(tarea: TareaCasosDemorados): TareaDiaria

    @Binds
    @IntoSet
    abstract fun bindTareaResumenDia(tarea: TareaResumenDia): TareaDiaria
}
