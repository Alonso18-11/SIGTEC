package com.softcorp.sigtec.feature.atencion.di

import com.softcorp.sigtec.core.sync.TareaDiaria
import com.softcorp.sigtec.feature.atencion.data.AtencionRepositoryImpl
import com.softcorp.sigtec.feature.atencion.data.TareaRepuestos
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AtencionModule {

    @Binds
    @Singleton
    abstract fun bindAtencionRepository(impl: AtencionRepositoryImpl): AtencionRepository

    // HU-18 (D-10): la revisión periódica corre la consulta diaria de repuestos
    @Binds
    @IntoSet
    abstract fun bindTareaRepuestos(tarea: TareaRepuestos): TareaDiaria
}
