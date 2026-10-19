package com.softcorp.sigtec.core.sync

import com.google.firebase.firestore.FirebaseFirestoreException
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento

internal const val TAREA_SUBIR = "SUBIR_CAMBIOS"
internal const val TAREA_BAJAR = "BAJAR_INCIDENCIAS"

internal fun debeReemplazarLocal(local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean =
    local == null ||
        !local.pendienteSincronizar ||
        remota.actualizadoEn.isAfter(local.actualizadoEn)

internal fun atencionAlBajar(local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean =
    local != null && (
        local.necesitaAtencion ||
            (local.pendienteSincronizar && remota.actualizadoEn.isAfter(local.actualizadoEn))
        )

internal fun debeGuardarMovimiento(local: MovimientoEntity?): Boolean =
    local == null || !local.pendienteSincronizar

internal fun cambiosDeCarga(
    tecnicoServidor: String?,
    cerradaServidor: Boolean,
    tecnicoLocal: String?,
    cerradaLocal: Boolean
): Map<String, Int> {
    val activoServidor = tecnicoServidor?.takeUnless { cerradaServidor }
    val activoLocal = tecnicoLocal?.takeUnless { cerradaLocal }
    if (activoServidor == activoLocal) return emptyMap()
    return buildMap {
        activoServidor?.let { put(it, -1) }
        activoLocal?.let { put(it, +1) }
    }
}

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

/**
 * HU-07 (RF-702, RF-703, RF-705): avisar solo si el cambio se observó en este celular: la incidencia
 * ya estaba aquí sin ser de esta persona y ahora le llega asignada y sin cerrar. Si llega por primera
 * vez (por ejemplo, en la primera descarga tras iniciar sesión) no se avisa, para no llenar la pantalla.
 */
internal fun esAsignacionNuevaPara(usuarioId: String, local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean =
    local != null &&
        local.tecnicoAsignadoId != usuarioId &&
        remota.tecnicoAsignadoId == usuarioId &&
        remota.marca != MarcaSeguimiento.CERRADA
