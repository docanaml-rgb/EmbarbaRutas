package com.embarba.rutas.datos

import com.embarba.rutas.modelos.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RepositorioUsuarios(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun obtenerUsuarioActual(onResult: (Result<Usuario>) -> Unit) {
        val uid = firebaseAuth.currentUser?.uid
            ?: run {
                onResult(Result.failure(Exception("No hay un usuario autenticado.")))
                return
            }

        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                if (!documento.exists()) {
                    onResult(Result.failure(Exception("No se encontró el perfil del usuario.")))
                    return@addOnSuccessListener
                }

                val nombre = documento.getString("nombre")
                val correo = documento.getString("correo")
                val rol = documento.getString("rol")

                if (nombre == null || correo == null || rol == null) {
                    onResult(Result.failure(Exception("El perfil del usuario está incompleto.")))
                } else {
                    onResult(Result.success(Usuario(nombre, correo, rol)))
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
