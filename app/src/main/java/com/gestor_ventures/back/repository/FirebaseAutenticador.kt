package com.gestor_ventures.back.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Traduce las excepciones de Firebase Authentication a [ResultadoAutenticador].
 *
 * Firebase guarda y hashea la contraseña de forma segura; esta clase no ve ni guarda esa
 * contraseña en ningún momento, solo se la pasa al SDK.
 */
@Singleton
class FirebaseAutenticador @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : Autenticador {

    override suspend fun crearCuenta(correo: String, contrasena: String): ResultadoAutenticador =
        try {
            firebaseAuth.createUserWithEmailAndPassword(correo, contrasena).await()
            ResultadoAutenticador.Exito
        } catch (e: FirebaseAuthUserCollisionException) {
            ResultadoAutenticador.CorreoYaRegistrado
        } catch (e: Exception) {
            ResultadoAutenticador.ErrorDeRed
        }

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticador =
        try {
            firebaseAuth.signInWithEmailAndPassword(correo, contrasena).await()
            ResultadoAutenticador.Exito
        } catch (e: FirebaseAuthInvalidUserException) {
            ResultadoAutenticador.CorreoNoRegistrado
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            ResultadoAutenticador.CredencialesInvalidas
        } catch (e: Exception) {
            ResultadoAutenticador.ErrorDeRed
        }

    override suspend fun enviarCorreoDeRecuperacion(correo: String): ResultadoAutenticador =
        try {
            firebaseAuth.sendPasswordResetEmail(correo).await()
            ResultadoAutenticador.Exito
        } catch (e: FirebaseAuthInvalidUserException) {
            ResultadoAutenticador.CorreoNoRegistrado
        } catch (e: Exception) {
            ResultadoAutenticador.ErrorDeRed
        }

    override fun cerrarSesion() = firebaseAuth.signOut()

    override fun observarCorreoDeSesion(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.email) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}
