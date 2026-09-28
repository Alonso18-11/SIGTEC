package com.softcorp.sigtec.core.di

import android.content.Context
import androidx.room.Room
import com.softcorp.sigtec.core.database.SigtecDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SigtecDatabase =
        Room.databaseBuilder(context, SigtecDatabase::class.java, "sigtec.db")
            // Solo durante el desarrollo: si cambia el esquema, se borra y se vuelve a descargar de Firestore
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideIncidenciaDao(db: SigtecDatabase) = db.incidenciaDao()
    @Provides fun provideMovimientoDao(db: SigtecDatabase) = db.movimientoDao()
    @Provides fun provideInformeTecnicoDao(db: SigtecDatabase) = db.informeTecnicoDao()
    @Provides fun provideSolicitudRepuestoDao(db: SigtecDatabase) = db.solicitudRepuestoDao()
    @Provides fun provideEvidenciaDao(db: SigtecDatabase) = db.evidenciaDao()
    @Provides fun provideFallaDiccionarioDao(db: SigtecDatabase) = db.fallaDiccionarioDao()
    @Provides fun provideSugerenciaIADao(db: SigtecDatabase) = db.sugerenciaIADao()
    @Provides fun provideCatalogoDao(db: SigtecDatabase) = db.catalogoDao()
    @Provides fun provideSincronizacionDao(db: SigtecDatabase) = db.sincronizacionDao()
}