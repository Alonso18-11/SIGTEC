package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.domain.model.Perfil
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * HU-18 (D-2): las tareas diarias y la hora local desde la que corre cada una, todos los días. Es la única constante de
 * horas: cambiar una hora es cambiar esta línea.
 */
enum class TipoTareaDiaria(val desde: LocalTime) {
    REPUESTOS(LocalTime.of(8, 0)),
    CASOS_DEMORADOS(LocalTime.of(8, 0)),
    RESUMEN_DIA(LocalTime.of(18, 0))
}

/**
 * HU-18 (D-1, D-10): una tarea que la revisión periódica de la HU-17 corre una vez al día. Cada módulo de negocio
 * registra la suya con `@IntoSet`, así `core/sync` no depende de ellos. Cada tarea anota su propio éxito (RF-1803).
 */
interface TareaDiaria {
    val tipo: TipoTareaDiaria

    /** RF-1810: si antes de correr hay que bajar del servidor las incidencias y los movimientos del día. */
    val necesitaDatosAlDia: Boolean

    /** RF-1802: el perfil que la necesita, con `perfil.puede(...)`. */
    fun correPara(perfil: Perfil): Boolean

    /** La última vez que terminó con éxito, o null si nunca (o si Room se borró). */
    suspend fun ultimaExitosa(): Instant?

    /** Corre la tarea; true si terminó con éxito y ya no hay que repetirla hoy. */
    suspend fun ejecutar(ahora: Instant, zona: ZoneId): Boolean
}

/** Declara el conjunto de tareas diarias aunque ningún módulo aporte una, para que Hilt siempre pueda inyectarlo. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TareasDiariasModule {
    @Multibinds
    abstract fun tareasDiarias(): Set<TareaDiaria>
}
