package com.embarba.rutas.datos

import com.google.firebase.auth.FirebaseAuth

class RepositorioAutenticacion(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun signIn(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(task.exception ?: Exception("No se pudo iniciar sesión.")))
                }
            }
    }
}
