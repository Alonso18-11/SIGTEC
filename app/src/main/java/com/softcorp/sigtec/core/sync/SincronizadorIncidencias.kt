package com.softcorp.sigtec.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.InformeTecnicoEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Sube a Firestore lo que quedó pendiente en Room (base de la HU-17). */
@Singleton
class SincronizadorIncidencias @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val baseLocal: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val informeTecnicoDao: InformeTecnicoDao,
    private val sincronizacionDao: SincronizacionDao
) {
    private val incidencias get() = firestore.collection("incidencias")
    private val movimientos get() = firestore.collection("movimientos")
    private val informes get() = firestore.collection("informes")
    private val contador get() = firestore.collection("contadores").document("incidencias")
    private val usuarios get() = firestore.collection("usuarios")

    private enum class ResultadoSubida { SUBIDA, CARGA_RECHAZADA }

    /** Sube todo lo pendiente. Devuelve false si algo debe reintentarse. */
    suspend fun subirPendientes(ahora: Instant): Boolean {
        var todoBien = true
        for (incidencia in incidenciaDao.pendientesDeSincronizar()) {
            todoBien = subir(incidencia) && todoBien
        }
        // Después de las incidencias: los que esperan a una incidencia que no subió siguen pendientes (RF-68)
        for (movimiento in movimientoDao.listosParaSubir()) {
            todoBien = subir(movimiento) && todoBien
        }
        // Después de los movimientos: el informe espera a su incidencia igual que ellos (RF-825)
        for (informe in informeTecnicoDao.listosParaSubir()) {
            todoBien = subir(informe) && todoBien
        }
        sincronizacionDao.registrar(
            SincronizacionEntity(tipoTarea = TAREA_SUBIR, ejecutadaEn = ahora, exitosa = todoBien)
        )
        sincronizacionDao.recortar(TAREA_SUBIR)
        return todoBien
    }

    suspend fun hayPendientes(): Boolean =
        incidenciaDao.pendientesDeSincronizar().isNotEmpty() ||
            movimientoDao.pendientesDeSincronizar().isNotEmpty() ||
            informeTecnicoDao.pendientesDeSincronizar().isNotEmpty()

    private suspend fun subir(local: IncidenciaEntity): Boolean {
        try {
            val numero = if (local.numero == null) {
                crearConNumero(local)
            } else {
                // RF-621: sin cupo en el servidor es un rechazo permanente; no se reintenta
                if (actualizarConCarga(local, local.numero) == ResultadoSubida.CARGA_RECHAZADA) {
                    return rechazar(local)
                }
                local.numero
            }
            // El número se anota siempre: la pantalla muestra INC-XXXX apenas existe en el servidor
            incidenciaDao.asignarNumero(local.id, numero)
            // Si alguien la modificó mientras se subía, sigue pendiente y se sube de nuevo
            incidenciaDao.marcarSincronizada(local.id, local.actualizadoEn)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo subir la incidencia ${local.id}", e)
            // Un rechazo permanente ya no se reintenta; uno pasajero sí
            return if (esErrorPermanente(e)) rechazar(local) else false
        }
    }

    /**
     * RF-620 (HU-06) y RF-822 (HU-08): sube una incidencia que ya existe y, si su técnico cambió
     * respecto del servidor o se cerró, mueve la carga de ambos en la misma transacción: o se aplica
     * todo, o nada. Las reglas de Firestore exigen ese movimiento con `getAfter()` (RF-622, RF-823). Si
     * la carga nueva de algún técnico saldría de 0..límite (RF-621), no escribe nada y devuelve
     * CARGA_RECHAZADA (D-7 del plan).
     */
    private suspend fun actualizarConCarga(local: IncidenciaEntity, numero: Int): ResultadoSubida =
        firestore.runTransaction { tx ->
            val documento = incidencias.document(local.id)
            val remotaSnap = tx.get(documento)
            val cambios = cambiosDeCarga(
                remotaSnap.getString("tecnicoAsignadoId"),
                remotaSnap.getString("marca") == MarcaSeguimiento.CERRADA.name,
                local.tecnicoAsignadoId,
                local.marca == MarcaSeguimiento.CERRADA
            )
            // Firestore exige hacer todas las lecturas antes de la primera escritura
            val perfiles = cambios.keys.associateWith { tx.get(usuarios.document(it)) }
            val cargas = cambios.mapValues { (id, delta) ->
                val perfil = perfiles.getValue(id)
                val actual = perfil.getLong("incidenciasActivas") ?: 0L
                if (!cargaValida(actual, perfil.getLong("limiteAtencion") ?: 0L, delta)) {
                    return@runTransaction ResultadoSubida.CARGA_RECHAZADA
                }
                actual + delta
            }
            cargas.forEach { (id, carga) -> tx.update(usuarios.document(id), "incidenciasActivas", carga) }
            tx.set(documento, local.aFirestore(numero), SetOptions.merge())
            ResultadoSubida.SUBIDA
        }.await()

    /**
     * RF-75 (HU-05) y RF-825 (HU-08): marca la incidencia, sus movimientos y su informe técnico
     * pendientes juntos, para que ninguno suba, y deja en el celular la versión del servidor.
     * Devuelve false solo si Room falló.
     */
    private suspend fun rechazar(local: IncidenciaEntity): Boolean = try {
        val marcada = baseLocal.withTransaction {
            // 0 filas: se editó mientras se subía y queda pendiente con la versión nueva
            val marcada = incidenciaDao.marcarNecesitaAtencion(local.id, local.actualizadoEn) > 0
            if (marcada) {
                movimientoDao.marcarNecesitaAtencionDe(local.id)
                informeTecnicoDao.marcarNecesitaAtencionDe(local.id)
            }
            marcada
        }
        if (marcada) traerVersionDelServidor(local.id)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo marcar el rechazo de la incidencia ${local.id}", e)
        false
    }

    /** Si falla, no pasa nada: la escucha en tiempo real traerá la misma versión y conserva la marca (RF-79). */
    private suspend fun traerVersionDelServidor(id: String) {
        try {
            val remota = incidencias.document(id).get(Source.SERVER).await().aIncidenciaEntity() ?: return
            baseLocal.withTransaction {
                // Solo si sigue rechazada: una edición hecha justo después no se pisa
                val actual = incidenciaDao.porId(id)
                if (actual != null && actual.necesitaAtencion && !actual.pendienteSincronizar) {
                    incidenciaDao.guardar(remota.copy(necesitaAtencion = true))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo traer la versión del servidor de la incidencia $id", e)
        }
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
            val ultimoActual = tx.get(contador).getLong("ultimo")
            val numero = siguienteNumero(ultimoActual).toInt()
            tx.set(contador, mapOf("ultimo" to numero))
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
        if (esErrorPermanente(e)) {
            movimientoDao.marcarNecesitaAtencion(movimiento.id)
            true
        } else {
            false
        }
    }

    /**
     * RF-825: el documento se llama como la incidencia (`informe.incidenciaId`), no como el informe
     * (D-5 del plan): un id predecible evita tener que buscarlo por otro campo. Igual que los
     * movimientos, las reglas prohíben editar informes: si ya existe, solo se anota como subido.
     */
    private suspend fun subir(informe: InformeTecnicoEntity): Boolean = try {
        val documento = informes.document(informe.incidenciaId)
        if (!documento.get().await().exists()) documento.set(informe.aFirestore()).await()
        informeTecnicoDao.marcarSincronizado(informe.id)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo subir el informe técnico ${informe.id}", e)
        if (esErrorPermanente(e)) {
            informeTecnicoDao.marcarNecesitaAtencion(informe.id)
            true
        } else {
            false
        }
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
