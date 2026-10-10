package com.softcorp.sigtec.core.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.di.ApplicationScope
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.perfilDesdeTexto
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.PreferenciasSeguridadRepository
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryFirebase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val baseLocal: SigtecDatabase,
    private val preferencias: PreferenciasSeguridadRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) : SesionRepository {

    private val usuarios get() = firestore.collection("usuarios")

    // Sesión de Firebase -> perfil en Firestore -> Usuario del dominio.
    // flatMapLatest: si cambia la sesión, se deja de escuchar el perfil anterior.
    @OptIn(ExperimentalCoroutinesApi::class)
    override val usuarioActual: StateFlow<Usuario?> =
        uidActual()
            .flatMapLatest { uid -> if (uid == null) flowOf(null) else perfilDe(uid) }
            .stateIn(scope, SharingStarted.Eagerly, initialValue = null)

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> {
        var autenticado = false   // true recién tras un signIn exitoso: RF-5b exige signOut() en todo fallo posterior
        return try {
            val uid = auth.signInWithEmailAndPassword(correo, contrasena).await().user?.uid
                ?: return Resultado.Error(ErrorDominio.CredencialesInvalidas)
            autenticado = true

            // RF-11: si entra otra persona, el borrado lo hace core/sync antes de volver a descargar (D-12)

            val usuario = usuarios.document(uid).get().await().aUsuario()
            if (usuario == null) {
                auth.signOut()   // tiene cuenta pero no perfil: no se deja una sesión a medias
                Resultado.Error(ErrorDominio.PerfilNoConfigurado)
            } else {
                Resultado.Exito(usuario)
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Resultado.Error(ErrorDominio.CredencialesInvalidas)
        } catch (e: FirebaseAuthInvalidUserException) {
            Resultado.Error(ErrorDominio.CredencialesInvalidas)
        } catch (e: FirebaseFirestoreException) {
            // Solo ocurre al leer usuarios/{uid}, es decir, ya autenticado: RF-5b
            auth.signOut()
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                Resultado.Error(ErrorDominio.SinConexion)   // RF-5: sin conexión al leer el perfil
            } else {
                Log.w(TAG, "Error no clasificado al leer el perfil", e)
                Resultado.Error(ErrorDominio.Desconocido(e.message))
            }
        } catch (e: FirebaseNetworkException) {
            if (autenticado) auth.signOut()   // RF-5b
            Resultado.Error(ErrorDominio.SinConexion)
        } catch (e: FirebaseTooManyRequestsException) {
            Resultado.Error(ErrorDominio.DemasiadosIntentos)
        } catch (e: CancellationException) {
            throw e   // nunca tragarse la cancelación de una corrutina; no hace signOut()
        } catch (e: Exception) {
            if (autenticado) auth.signOut()   // RF-5b
            Log.w(TAG, "Error no clasificado al iniciar sesión", e)
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    override suspend fun cerrarSesion() {
        // HU-01: "al cerrar sesión se borra del celular la información del usuario"
        withContext(io) { baseLocal.clearAllTables() }
        auth.signOut()
    }

    // RF-15: el cierre que dispara la revocación remota debe desvincular la huella
    // igual que el caso de uso CerrarSesion, cuando el perfil revocado era su dueño.
    private suspend fun cerrarSesionPorRevocacion(uidRevocado: String) {
        val prefs = preferencias.preferenciasHuella.first()
        if (prefs.esPropietario(uidRevocado)) preferencias.limpiar()
        cerrarSesion()
    }

    private fun uidActual(): Flow<String?> = callbackFlow {
        val oyente = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(oyente)
        awaitClose { auth.removeAuthStateListener(oyente) }
    }.distinctUntilChanged()

    private fun perfilDe(uid: String): Flow<Usuario?> = callbackFlow {
        // RF-15: solo dispara el cierre si antes hubo un perfil válido con la sesión ya abierta.
        // Si nunca lo hubo (perfil nulo/inválido desde el primer valor), es el ingreso normal
        // de RF-7 y lo resuelve iniciarSesion(); este listener no debe interferir con ese flujo.
        var huboPerfilValido = false
        val registro = usuarios.document(uid).addSnapshotListener { doc, error ->
            if (error == null) {
                val usuario = doc?.aUsuario()
                if (usuario != null) {
                    huboPerfilValido = true
                } else if (huboPerfilValido) {
                    huboPerfilValido = false   // evita repetir el cierre si llegan más eventos antes del signOut()
                    scope.launch(io) { cerrarSesionPorRevocacion(uid) }
                }
                trySend(usuario)
            }
        }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        val perfil = perfilDesdeTexto(getString("perfil")) ?: return null
        return Usuario(
            id = id,
            nombre = getString("nombre").orEmpty(),
            correo = getString("correo").orEmpty(),
            area = getString("area").orEmpty(),
            cargo = getString("cargo").orEmpty(),
            perfil = perfil
        )
    }

    private companion object {
        const val TAG = "SesionRepository"
    }
}