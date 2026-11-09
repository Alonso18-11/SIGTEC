package com.softcorp.sigtec.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.softcorp.sigtec.core.data.AlmacenFotos
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.FallaDiccionarioDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.dao.SolicitudRepuestoDao
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import com.softcorp.sigtec.core.database.entity.toDomain
import com.softcorp.sigtec.core.di.ApplicationScope
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.debeBorrarDatosAlEntrar
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import com.softcorp.sigtec.core.notificaciones.NotificadorAsignaciones
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SincronizacionTiempoReal @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val baseLocal: SigtecDatabase,
    private val almacen: AlmacenFotos,
    private val sesion: SesionRepository,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,
    private val sincronizacionDao: SincronizacionDao,
    private val solicitudRepuestoDao: SolicitudRepuestoDao,
    private val fallaDiccionarioDao: FallaDiccionarioDao,
    private val programador: ProgramadorSincronizacion,
    private val reloj: Reloj,
    private val notificador: NotificadorAsignaciones,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun iniciar() {
        var duenoDatos: String? = null
        sesion.usuarioActual
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid == null) {
                    emptyFlow()
                } else {
                    flow {
                        if (debeBorrarDatosAlEntrar(duenoDatos, uid)) {
                            withContext(io) {
                                baseLocal.clearAllTables()
                                borrarFotos()   // RF-942: después de Room, igual que al cerrar sesión
                            }
                        }
                        duenoDatos = uid
                        emitAll(
                            merge(
                                escuchar("incidencias", { snap -> guardarIncidencias(snap, uid) }),
                                escuchar("movimientos", ::guardarMovimientos),
                                escuchar("equipos", ::guardarEquipos),
                                // HU-11 (RF-1120): el detalle muestra las solicitudes de repuesto a todos los perfiles
                                escuchar("solicitudesRepuesto", ::guardarSolicitudes),
                                // HU-13 (RF-1322): el diccionario lo leen el técnico y el personal de sistemas
                                escuchar("fallas", ::guardarFallas),
                                // RF-619 (HU-06): solo los técnicos; quien deja de serlo llega como REMOVED
                                escuchar(
                                    "usuarios", ::guardarTecnicos,
                                    firestore.collection("usuarios").whereEqualTo("perfil", "TECNICO")
                                ),
                                vigilarPendientes()
                            )
                        )
                    }
                }
            }
            .launchIn(scope)
    }

    // `consulta` permite escuchar solo una parte de la colección, como los técnicos de /usuarios
    private fun escuchar(
        coleccion: String,
        guardar: suspend (QuerySnapshot) -> Unit,
        consulta: Query = firestore.collection(coleccion)
    ): Flow<Unit> = callbackFlow {
        val registro = consulta.addSnapshotListener { snap, error ->
            if (error != null) Log.w(TAG, "Error al escuchar $coleccion", error)
            else if (snap != null) trySend(snap)
        }
        awaitClose { registro.remove() }
    }.map { snap ->
        runCatching { guardar(snap) }
            .onFailure { Log.w(TAG, "Error al guardar $coleccion en Room", it) }
        // Solo cuenta como descarga si los datos vienen del servidor, no del caché
        if (!snap.metadata.isFromCache) registrarDescarga()
    }

    private suspend fun guardarIncidencias(snap: QuerySnapshot, uid: String) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // cambio propio aún sin confirmar
            val remota = documento.aIncidenciaEntity() ?: continue
            val local = incidenciaDao.porId(remota.id)
            if (debeReemplazarLocal(local, remota)) {
                baseLocal.withTransaction {
                    incidenciaDao.guardar(remota.copy(necesitaAtencion = atencionAlBajar(local, remota)))
                    // RF-79: si se pierde un cambio sin subir, ningún movimiento de ese intento debe subir
                    if (local?.pendienteSincronizar == true) movimientoDao.marcarNecesitaAtencionDe(remota.id)
                }
                // HU-07 (RF-702): después de guardar, así al tocar el aviso el detalle ya tiene la versión nueva
                if (esAsignacionNuevaPara(uid, local, remota)) notificador.avisar(remota.toDomain(), uid)
            }
        }
    }

    private suspend fun guardarMovimientos(snap: QuerySnapshot) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val remoto = cambio.document.aMovimientoEntity() ?: continue
            if (debeGuardarMovimiento(movimientoDao.porId(remoto.id))) movimientoDao.guardar(remoto)
        }
    }

    private suspend fun guardarSolicitudes(snap: QuerySnapshot) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // cambio propio aún sin confirmar
            val remota = documento.aSolicitudEntity() ?: continue
            if (debeReemplazarSolicitud(solicitudRepuestoDao.porId(remota.id), remota)) solicitudRepuestoDao.guardar(remota)
        }
    }

    private suspend fun guardarFallas(snap: QuerySnapshot) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // falla propia aún sin confirmar
            val remota = documento.aFallaEntity() ?: continue
            if (debeReemplazarFalla(fallaDiccionarioDao.porId(remota.id), remota)) fallaDiccionarioDao.guardar(remota)
        }
    }

    // RF-41: /equipos es de solo lectura, sin lógica de conflictos ni pendientes de sincronizar.
    private suspend fun guardarEquipos(snap: QuerySnapshot) {
        val nuevos = snap.documentChanges
            .filter { it.type != DocumentChange.Type.REMOVED }
            .mapNotNull { it.document.aEquipoEntity() }
        if (nuevos.isNotEmpty()) catalogoDao.guardarEquipos(nuevos)
    }

    // RF-619 (HU-06): también de solo lectura. Un técnico mal formado se descarta sin frenar al resto.
    private suspend fun guardarTecnicos(snap: QuerySnapshot) {
        val (quitados, cambiados) = snap.documentChanges.partition { it.type == DocumentChange.Type.REMOVED }
        val nuevos = cambiados.mapNotNull { it.document.aTecnicoEntity() }
        if (nuevos.isNotEmpty()) catalogoDao.guardarTecnicos(nuevos)
        if (quitados.isNotEmpty()) catalogoDao.borrarTecnicos(quitados.map { it.document.id })
    }

    private fun vigilarPendientes(): Flow<Unit> =
        // HU-11: también las solicitudes, que quedan pendientes solas cuando llega el número de Logística
        combine(
            incidenciaDao.observarPendientes(),
            movimientoDao.observarPendientes(),
            solicitudRepuestoDao.observarPendientes()
        ) { i, m, s ->
            i + m + s > 0
        }
            .distinctUntilChanged()
            .filter { it }
            .map { programador.subirCambios() }

    private suspend fun registrarDescarga() {
        sincronizacionDao.registrar(
            SincronizacionEntity(tipoTarea = TAREA_BAJAR, ejecutadaEn = reloj.ahora(), exitosa = true)
        )
        sincronizacionDao.recortar(TAREA_BAJAR)
    }

    private fun borrarFotos() {
        try {
            almacen.borrarTodo()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron borrar las fotos de evidencia", e)
        }
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
