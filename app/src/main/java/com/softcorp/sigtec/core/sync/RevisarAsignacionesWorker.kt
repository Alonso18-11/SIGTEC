package com.softcorp.sigtec.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.softcorp.sigtec.core.database.dao.FallaDiccionarioDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.dao.SolicitudRepuestoDao
import com.softcorp.sigtec.core.database.dao.SugerenciaIADao
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * HU-17: la revisión que corre cada 15 minutos aunque la app esté cerrada. Programa la subida si quedó algo pendiente
 * (RF-1706) y, si quien tiene la sesión atiende incidencias, baja sus asignaciones activas y avisa de las nuevas
 * (RF-1708 a RF-1714). Siempre termina con éxito: la siguiente revisión ya es el reintento (RF-1704, RF-1715).
 */
@HiltWorker
class RevisarAsignacionesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sesion: SesionRepository,
    private val monitor: MonitorConexion,
    private val firestore: FirebaseFirestore,
    private val guardado: GuardadoIncidencias,
    private val inicioSesion: InicioSesionLocal,
    private val programador: ProgramadorSincronizacion,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val solicitudRepuestoDao: SolicitudRepuestoDao,
    private val fallaDiccionarioDao: FallaDiccionarioDao,
    private val sugerenciaIADao: SugerenciaIADao,
    private val sincronizacionDao: SincronizacionDao,
    private val reloj: Reloj
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Con la app cerrada, el perfil tarda en llegar de Firestore; sin sesión no se hace nada (RF-1703)
        val usuario = withTimeoutOrNull(ESPERA_SESION_MS) { sesion.usuarioActual.filterNotNull().first() }
            ?: return Result.success()

        if (hayPendientes()) programador.subirCambios()   // RF-1706, RF-1707: respaldo para todos los perfiles

        if (!debeConsultarAsignaciones(usuario.perfil)) return Result.success()   // RF-1716
        if (!debeIntentarSubir(monitor.hayConexionAhora())) return Result.success()   // RF-1704

        revisarAsignaciones(usuario)
        return Result.success()
    }

    private suspend fun hayPendientes(): Boolean = hayPendientesDeSubir(
        incidenciaDao.observarPendientes().first(),
        movimientoDao.observarPendientes().first(),
        solicitudRepuestoDao.observarPendientes().first(),
        fallaDiccionarioDao.observarCantidadPendientes().first(),
        sugerenciaIADao.observarCantidadPendientes().first()
    )

    private suspend fun revisarAsignaciones(usuario: Usuario) {
        try {
            // RF-1708 (D-2): solo las suyas sin cerrar, y del servidor, no de la caché de Firestore
            val documentos = firestore.collection("incidencias")
                .whereEqualTo("tecnicoAsignadoId", usuario.id)
                .whereIn("marca", MARCAS_ACTIVAS_ASIGNADAS.map { it.name })
                .get(Source.SERVER)
                .await()
                .documents
            val inicio = inicioSesion.leer()
            for (documento in documentos) {
                val remota = documento.aIncidenciaEntity() ?: continue   // RF-1709 (RF-60): mal formada, se salta
                guardado.guardar(remota, usuario.id, inicio)              // RF-1709 a RF-1714
            }
            registrarRevision()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron revisar las asignaciones", e)   // RF-1715: la siguiente lo vuelve a intentar
        }
    }

    // RF-1717 (D-6): permite comprobar que la revisión corrió con la app cerrada
    private suspend fun registrarRevision() {
        sincronizacionDao.registrar(
            SincronizacionEntity(tipoTarea = TAREA_REVISAR, ejecutadaEn = reloj.ahora(), exitosa = true)
        )
        sincronizacionDao.recortar(TAREA_REVISAR)
    }

    private companion object {
        const val TAG = "RevisarAsignaciones"
        const val ESPERA_SESION_MS = 10_000L
    }
}
