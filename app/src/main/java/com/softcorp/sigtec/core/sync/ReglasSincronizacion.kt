package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity

internal const val TAREA_SUBIR = "SUBIR_CAMBIOS"
internal const val TAREA_BAJAR = "BAJAR_INCIDENCIAS"

/**
 * Regla de conflictos al bajar de Firestore: nunca se pisa un cambio local
 * que todavía no se subió, salvo que la versión remota sea más reciente.
 * Limitación conocida: compara la hora de cada celular (con hora automática, la diferencia es mínima).
 */
internal fun debeReemplazarLocal(local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean =
    local == null ||
        !local.pendienteSincronizar ||
        remota.actualizadoEn.isAfter(local.actualizadoEn)

/** Los movimientos no se editan: solo se agregan si faltan. */
internal fun debeGuardarMovimiento(local: MovimientoEntity?): Boolean =
    local == null || !local.pendienteSincronizar
