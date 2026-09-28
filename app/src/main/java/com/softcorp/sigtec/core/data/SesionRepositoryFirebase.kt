package com.softcorp.sigtec.core.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.softcorp.sigtec.core.database.SigtecDatabase
import com.softcorp.sigtec.core.di.ApplicationScope
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Usuario
import com.softcorp.sigtec.core.domain.repository.SesionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryFirebase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val baseLocal: SigtecDatabase,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope scope: CoroutineScope
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
        return try {
            val uid = auth.signInWithEmailAndPassword(correo, contrasena).await().user?.uid
                ?: return Resultado.Error(ErrorDominio.CredencialesInvalidas)

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
        } catch (e: FirebaseNetworkException) {
            Resultado.Error(ErrorDominio.SinConexion)
        } catch (e: FirebaseTooManyRequestsException) {
            Resultado.Error(ErrorDominio.DemasiadosIntentos)
        } catch (e: CancellationException) {
            throw e   // nunca tragarse la cancelación de una corrutina
        } catch (e: Exception) {
            Resultado.Error(ErrorDominio.Desconocido(e.message))
        }
    }

    override suspend fun cerrarSesion() {
        // HU-01: "al cerrar sesión se borra del celular la información del usuario"
        withContext(io) { baseLocal.clearAllTables() }
        auth.signOut()
    }

    private fun uidActual(): Flow<String?> = callbackFlow {
        val oyente = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(oyente)
        awaitClose { auth.removeAuthStateListener(oyente) }
    }.distinctUntilChanged()

    private fun perfilDe(uid: String): Flow<Usuario?> = callbackFlow {
        val registro = usuarios.document(uid).addSnapshotListener { doc, error ->
            if (error == null) trySend(doc?.aUsuario())
        }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        val perfil = getString("perfil")
            ?.let { runCatching { Perfil.valueOf(it) }.getOrNull() }
            ?: return null
        return Usuario(
            id = id,
            nombre = getString("nombre").orEmpty(),
            correo = getString("correo").orEmpty(),
            area = getString("area").orEmpty(),
            cargo = getString("cargo").orEmpty(),
            perfil = perfil
        )
    }
}