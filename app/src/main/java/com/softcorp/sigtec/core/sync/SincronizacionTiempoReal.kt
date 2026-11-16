package com.softcorp.sigtec.core.sync

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.softcorp.sigtec.core.data.AlmacenFotos
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.database.dao.FallaDiccionarioDao
import com.softcorp.sigtec.core.database.dao.IncidenciaDao
import com.softcorp.sigtec.core.database.dao.InformeTecnicoDao
import com.softcorp.sigtec.core.database.dao.MovimientoDao
import com.softcorp.sigtec.core.database.dao.SincronizacionDao
import com.softcorp.sigtec.core.database.dao.SolicitudRepuestoDao
import com.softcorp.sigtec.core.database.dao.SugerenciaIADao
import com.softcorp.sigtec.core.database.entity.SincronizacionEntity
import com.softcorp.sigtec.core.di.ApplicationScope
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.debeBorrarDatosAlEntrar
import com.softcorp.sigtec.core.domain.repository.SesionRepository
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
    private val informeTecnicoDao: InformeTecnicoDao,
    private val catalogoDao: CatalogoDao,
    private val sincronizacionDao: SincronizacionDao,
    private val solicitudRepuestoDao: SolicitudRepuestoDao,
    private val fallaDiccionarioDao: FallaDiccionarioDao,
    private val sugerenciaIADao: SugerenciaIADao,
    private val programador: ProgramadorSincronizacion,
    private val reloj: Reloj,
    private val guardado: GuardadoIncidencias,
    private val inicioSesion: InicioSesionLocal,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    @param:ApplicationScope private val scope: CoroutineScope
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
                        inicioSesion.anotarSiFalta(reloj.ahora())   // HU-17 (D-1): después del borrado de D-12
                        emitAll(
                            merge(
                                escuchar("incidencias", { snap -> guardarIncidencias(snap, uid) }),
                                escuchar("movimientos", ::guardarMovimientos),
                                // HU-16 (RF-1623): el historial del equipo muestra los informes de todos los técnicos
                                escuchar("informes", ::guardarInformes),
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
        // INCLUDE: Firestore avisa también cuando confirma con el servidor lo que antes dio desde su caché. Sin eso, al
        // iniciar sesión con la caché llena y sin cambios en el servidor nunca llegaba una lista «del servidor» y la
        // descarga no se anotaba. `documentChanges` sigue sin traer los cambios que son solo de metadatos.
        val registro = consulta.addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
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
        val inicio = inicioSesion.leer()   // HU-17 (D-1): para avisar de la que llega ya asignada tras el ingreso
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // cambio propio aún sin confirmar
            val remota = documento.aIncidenciaEntity() ?: continue
            guardado.guardar(remota, uid, inicio)
        }
    }

    private suspend fun guardarMovimientos(snap: QuerySnapshot) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val remoto = cambio.document.aMovimientoEntity() ?: continue
            if (debeGuardarMovimiento(movimientoDao.porId(remoto.id))) movimientoDao.guardar(remoto)
        }
    }

    private suspend fun guardarInformes(snap: QuerySnapshot) {
        for (cambio in snap.documentChanges) {
            if (cambio.type == DocumentChange.Type.REMOVED) continue
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // informe propio aún sin confirmar
            val remoto = documento.aInformeEntity() ?: continue
            // RF-1624: si el celular ya tiene el informe de esa incidencia (subido o no), se queda con el suyo
            if (debeGuardarInforme(informeTecnicoDao.porIncidencia(remoto.incidenciaId))) informeTecnicoDao.guardar(remoto)
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
            // La app no borra fallas (D-4), pero el equipo puede corregir una desde la consola de Firebase: se quita
            // también del celular, salvo que tenga algo sin subir
            if (cambio.type == DocumentChange.Type.REMOVED) {
                fallaDiccionarioDao.borrarSincronizada(cambio.document.id)
                continue
            }
            val documento = cambio.document
            if (documento.metadata.hasPendingWrites()) continue   // falla propia aún sin confirmar
            val remota = documento.aFallaEntity() ?: continue
            if (debeReemplazarFalla(fallaDiccionarioDao.porId(remota.id), remota)) fallaDiccionarioDao.guardar(remota)
        }
        // Si el borrado no llegó como REMOVED (la app no escuchaba, o lo ignoró una versión anterior), se quitan las
        // fallas ya sincronizadas que no están en la lista. Esa lista puede venir de la caché de Firestore, que es de
        // donde salieron las de Room; vacía y desde la caché no se usa, por si la caché se perdió
        if (!snap.isEmpty || !snap.metadata.isFromCache) {
            fallaDiccionarioDao.borrarSincronizadasFueraDe(snap.documents.map { it.id })
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
        // HU-11: también las solicitudes, que quedan pendientes solas cuando llega el número de Logística.
        // HU-13 (RF-1321): y las fallas del diccionario; sin esto, una falla nueva no programaba la subida.
        // HU-14 (RF-1429): y las sugerencias de IA ya decididas
        combine(
            incidenciaDao.observarPendientes(),
            movimientoDao.observarPendientes(),
            solicitudRepuestoDao.observarPendientes(),
            fallaDiccionarioDao.observarCantidadPendientes(),
            sugerenciaIADao.observarCantidadPendientes()
        ) { i, m, s, f, sug ->
            hayPendientesDeSubir(i, m, s, f, sug)
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
