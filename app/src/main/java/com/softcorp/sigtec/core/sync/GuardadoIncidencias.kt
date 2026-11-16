package com.softcorp.sigtec.core.sync

import androidx.room.withTransaction
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.notificaciones.NotificadorAsignaciones
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda en Room una incidencia que bajó de Firestore y avisa si es una asignación nueva. La usan la escucha en tiempo
 * real y la revisión periódica de la HU-17, que pueden correr a la vez en el mismo proceso: el candado hace que solo la
 * primera vea la asignación como nueva, así no se avisa dos veces (RF-1712).
 */
@Singleton
class GuardadoIncidencias @Inject constructor(
    private val baseLocal: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val notificador: NotificadorAsignaciones
) {
    private val candado = Mutex()

    suspend fun guardar(remota: IncidenciaEntity, usuarioId: String, inicioSesion: Instant?) = candado.withLock {
        val local = incidenciaDao.porId(remota.id)
        if (!debeReemplazarLocal(local, remota)) return@withLock   // RF-62
        baseLocal.withTransaction {
            incidenciaDao.guardar(remota.copy(necesitaAtencion = atencionAlBajar(local, remota)))
            // RF-79: si se pierde un cambio sin subir, ningún movimiento de ese intento debe subir
            if (local?.pendienteSincronizar == true) movimientoDao.marcarNecesitaAtencionDe(remota.id)
        }
        // HU-07 (RF-702), HU-17 (RF-1710): después de guardar, así al tocar el aviso el detalle ya tiene la versión nueva
        if (esAsignacionNuevaPara(usuarioId, local, remota, inicioSesion)) {
            notificador.avisar(remota.toDomain(), usuarioId)
        }
    }
}
