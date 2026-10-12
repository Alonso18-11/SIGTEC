package com.softcorp.sigtec.core.sync

import com.google.firebase.firestore.FirebaseFirestoreException
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

/**
 * RF-79: marca con que queda la incidencia al reemplazarla con la versión remota. Se conserva si ya
 * estaba rechazada, y se pone si tenía un cambio sin subir que pierde frente a una remota más reciente,
 * para que ese cambio no se pierda en silencio. No cambia la regla de conflictos (`debeReemplazarLocal`).
 */
internal fun atencionAlBajar(local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean =
    local != null && (
        local.necesitaAtencion ||
            (local.pendienteSincronizar && remota.actualizadoEn.isAfter(local.actualizadoEn))
        )

/** Los movimientos no se editan: solo se agregan si faltan. */
internal fun debeGuardarMovimiento(local: MovimientoEntity?): Boolean =
    local == null || !local.pendienteSincronizar

/**
 * RF-620 (HU-06): cuánto cambia la carga de cada técnico al subir una incidencia, comparando el
 * técnico que tiene el servidor con el del celular. Mismo técnico: no cambia nada. Al cerrar, la
 * HU-08 debe ampliarla para restar también la carga (dependencia anotada en la spec 006).
 */
internal fun cambiosDeCarga(tecnicoServidor: String?, tecnicoLocal: String?): Map<String, Int> {
    if (tecnicoServidor == tecnicoLocal) return emptyMap()
    return buildMap {
        tecnicoServidor?.let { put(it, -1) }
        tecnicoLocal?.let { put(it, +1) }
    }
}

/** RF-621 (HU-06): la carga nueva queda entre 0 y el límite, lo mismo que exige la regla de /usuarios. */
internal fun cargaValida(actual: Long, limite: Long, delta: Int): Boolean = actual + delta in 0..limite

/** Siguiente número del contador `contadores/incidencias` para armar `INC-0124` (RF-69). */
internal fun siguienteNumero(ultimoActual: Long?): Long = (ultimoActual ?: 0L) + 1

/** Un error permanente no se reintenta: pasa a necesitar atención manual (RF-75). */
internal fun esErrorPermanente(error: Throwable): Boolean =
    error is FirebaseFirestoreException && esCodigoPermanente(error.code.name)

/**
 * La regla trabaja con el nombre del código de Firestore y no con el enum: así se prueba en la JVM
 * sin inicializar clases de Firebase que dependen de Android (RF-75).
 */
internal fun esCodigoPermanente(codigo: String): Boolean = codigo in CODIGOS_PERMANENTES

// FAILED_PRECONDITION no está: en una transacción indica que otro celular cambió el dato (se reintenta)
private val CODIGOS_PERMANENTES = setOf("PERMISSION_DENIED", "INVALID_ARGUMENT")

/** No se intenta subir sin una red validada: evita gastar reintentos en vano (RF-78). */
internal fun debeIntentarSubir(hayConexionValidada: Boolean): Boolean = hayConexionValidada
