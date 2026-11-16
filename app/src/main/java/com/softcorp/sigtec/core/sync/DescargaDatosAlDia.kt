package com.softcorp.sigtec.core.sync

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-18 (RF-1810, D-6): con la app cerrada, el Room del jefe solo se actualiza al abrirla. Antes de contar los casos
 * demorados o armar el resumen, baja del servidor todas las incidencias y los movimientos del día, y los guarda con
 * las reglas de siempre. Sin esto, contaría como pendiente una incidencia que otro celular ya cerró.
 */
@Singleton
class DescargaDatosAlDia @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val guardado: GuardadoIncidencias,
    private val inicioSesion: InicioSesionLocal,
    private val movimientoDao: MovimientoDao
) {

    /** true si bajó todo; false si la consulta falló, y entonces no se debe avisar con datos viejos (RF-1811). */
    suspend fun bajar(usuarioId: String, ahora: Instant, zona: ZoneId): Boolean = try {
        val incidencias = firestore.collection("incidencias").get(Source.SERVER).await().documents
        val inicio = inicioSesion.leer()
        for (documento in incidencias) {
            val remota = documento.aIncidenciaEntity() ?: continue   // RF-60: mal formada, se salta
            guardado.guardar(remota, usuarioId, inicio)
        }

        // Solo `fecha >= inicio del día`: un filtro de un campo, sin índice compuesto
        val inicioDelDia = ahora.atZone(zona).toLocalDate().atStartOfDay(zona).toInstant()
        val movimientos = firestore.collection("movimientos")
            .whereGreaterThanOrEqualTo("fecha", Timestamp(inicioDelDia))
            .get(Source.SERVER).await().documents
        for (documento in movimientos) {
            val remoto = documento.aMovimientoEntity() ?: continue
            if (debeGuardarMovimiento(movimientoDao.porId(remoto.id))) movimientoDao.guardar(remoto)   // RF-63
        }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudieron bajar los datos del día", e)
        false
    }

    private companion object {
        const val TAG = "DescargaDatosAlDia"
    }
}
