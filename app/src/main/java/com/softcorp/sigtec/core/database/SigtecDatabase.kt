package com.softcorp.sigtec.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.softcorp.sigtec.core.database.dao.*
import com.softcorp.sigtec.core.database.entity.*

@Database(
    entities = [
        IncidenciaEntity::class, MovimientoEntity::class, InformeTecnicoEntity::class,
        SolicitudRepuestoEntity::class, EvidenciaEntity::class, FallaDiccionarioEntity::class,
        SugerenciaIAEntity::class, TecnicoEntity::class, EquipoEntity::class,
        SincronizacionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Convertidores::class)
abstract class SigtecDatabase : RoomDatabase() {
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun informeTecnicoDao(): InformeTecnicoDao
    abstract fun solicitudRepuestoDao(): SolicitudRepuestoDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun fallaDiccionarioDao(): FallaDiccionarioDao
    abstract fun sugerenciaIADao(): SugerenciaIADao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun sincronizacionDao(): SincronizacionDao
}