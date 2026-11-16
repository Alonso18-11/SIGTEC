package com.softcorp.sigtec.core.sync

import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-17 (D-1, RF-1711): cuándo empezó en este celular la sesión de quien tiene los datos. Se guarda en la tabla de
 * sincronizaciones, que se borra con el resto de Room al cerrar sesión o al entrar otra persona, así que dura lo mismo
 * que esa sesión.
 */
@Singleton
class InicioSesionLocal @Inject constructor(
    private val sincronizacionDao: SincronizacionDao
) {

    /** Anota `ahora` si todavía no hay un inicio para esta sesión. */
    suspend fun anotarSiFalta(ahora: Instant) {
        if (leer() == null) {
            sincronizacionDao.registrar(
                SincronizacionEntity(tipoTarea = TAREA_INICIO_SESION, ejecutadaEn = ahora, exitosa = true)
            )
        }
    }

    suspend fun leer(): Instant? =
        sincronizacionDao.observarUltimaExitosa(TAREA_INICIO_SESION).first()?.ejecutadaEn
}
