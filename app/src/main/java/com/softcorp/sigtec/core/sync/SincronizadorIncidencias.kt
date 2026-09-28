package com.softcorp.sigtec.core.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Sube a Firestore lo que quedó pendiente en Room (base de la HU-17). */
@Singleton
class SincronizadorIncidencias @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val sincronizacionDao: SincronizacionDao
) {
    private val incidencias get() = firestore.collection("incidencias")
    private val movimientos get() = firestore.collection("movimientos")
    private val contador get() = firestore.collection("contadores").document("incidencias")

    /** Sube todo lo pendiente. Devuelve false si algo debe reintentarse. */
    suspend fun subirPendientes(): Boolean {
        var todoBien = true
        for (incidencia in incidenciaDao.pendientesDeSincronizar()) {
            todoBien = subir(incidencia) && todoBien
        }
        for (movimiento in movimientoDao.pendientesDeSincronizar()) {
            todoBien = subir(movimiento) && todoBien
        }
        sincronizacionDao.registrar(
            SincronizacionEntity(tipoTarea = TAREA_SUBIR, ejecutadaEn = Instant.now(), exitosa = todoBien)
        )
        sincronizacionDao.recortar(TAREA_SUBIR)
        return todoBien
    }

    suspend fun hayPendientes(): Boolean =
        incidenciaDao.pendientesDeSincronizar().isNotEmpty() ||
            movimientoDao.pendientesDeSincronizar().isNotEmpty()

    private suspend fun subir(local: IncidenciaEntity): Boolean = try {
        val numero = if (local.numero == null) {
            crearConNumero(local)
        } else {
            incidencias.document(local.id)
                .set(local.aFirestore(local.numero), SetOptions.merge())
                .await()
            local.numero
        }
        // El número se anota siempre: la pantalla muestra INC-XXXX apenas existe en el servidor
        incidenciaDao.asignarNumero(local.id, numero)
        // Si alguien la modificó mientras se subía, sigue pendiente y se sube de nuevo
        incidenciaDao.marcarSincronizada(local.id, local.actualizadoEn)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo subir la incidencia ${local.id}", e)
        false
    }

    /**
     * Transacción: si dos celulares suben a la vez, Firestore repite la transacción
     * y nunca entrega el mismo número. Si la incidencia ya existe (se subió pero la app
     * se cerró antes de anotarlo), reutiliza su número en lugar de gastar otro.
     */
    private suspend fun crearConNumero(local: IncidenciaEntity): Int =
        firestore.runTransaction { tx ->
            val documento = incidencias.document(local.id)
            val existente = tx.get(documento)
            if (existente.exists()) {
                return@runTransaction existente.getLong("numero")!!.toInt()
            }
            val numero = (tx.get(contador).getLong("ultimo") ?: 0L).toInt() + 1
            tx.update(contador, "ultimo", numero)
            tx.set(documento, local.aFirestore(numero))
            numero
        }.await()

    private suspend fun subir(movimiento: MovimientoEntity): Boolean = try {
        val documento = movimientos.document(movimiento.id)
        // Las reglas prohíben editar movimientos: si ya existe, solo se anota como subido
        if (!documento.get().await().exists()) documento.set(movimiento.aFirestore()).await()
        movimientoDao.marcarSincronizado(movimiento.id)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo subir el movimiento ${movimiento.id}", e)
        false
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
