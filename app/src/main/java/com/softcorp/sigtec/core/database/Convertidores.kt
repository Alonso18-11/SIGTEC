package com.softcorp.sigtec.core.database

import androidx.room.TypeConverter
import java.time.Instant

class Convertidores {
    @TypeConverter
    fun instantALong(valor: Instant?): Long? = valor?.toEpochMilli()

    @TypeConverter
    fun longAInstant(valor: Long?): Instant? = valor?.let(Instant::ofEpochMilli)
}