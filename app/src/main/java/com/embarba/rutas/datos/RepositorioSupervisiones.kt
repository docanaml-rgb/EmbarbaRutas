package com.embarba.rutas.datos

import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.modelos.ChecklistResult
import com.embarba.rutas.modelos.DetalleHistorial
import com.embarba.rutas.modelos.RegistroMovilidad
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MovilidadYaRegistradaException : IllegalStateException("La movilidad ya fue registrada.")
class ChecklistRequeridoException : IllegalStateException("Debes guardar el checklist antes de finalizar.")
class MovilidadRequeridaException : IllegalStateException("Debes registrar la movilidad antes de finalizar.")

data class RequisitosFinalizacion(
    val checklistGuardado: Boolean,
    val movilidadGuardada: Boolean
)

class RepositorioSupervisiones(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun obtenerSupervisionesAsignadas(onResult: (Result<List<Supervision>>) -> Unit) {
        val supervisorId = firebaseAuth.currentUser?.uid
            ?: run {
                onResult(Result.failure(Exception("No hay un usuario autenticado.")))
                return
            }

        firestore.collection("supervisiones")
            .whereEqualTo("supervisorId", supervisorId)
            .get()
            .addOnSuccessListener { resultado ->
                val supervisiones = resultado.documents.mapNotNull { documento ->
                    val cliente = documento.getString("cliente")
                    val direccion = documento.getString("direccion")
                    val distrito = documento.getString("distrito")
                    val hora = documento.getString("hora")
                    val estado = documento.getString("estado")
                    val latitud = documento.getDouble("latitud")
                    val longitud = documento.getDouble("longitud")

                    if (
                        cliente == null || direccion == null || distrito == null || hora == null ||
                        estado == null || latitud == null || longitud == null
                    ) {
                        null
                    } else {
                        Supervision(
                            id = documento.id,
                            cliente = cliente,
                            direccion = direccion,
                            distrito = distrito,
                            hora = hora,
                            estado = estado,
                            latitud = latitud,
                            longitud = longitud
                        )
                    }
                }.filter { supervision ->
                    supervision.estado == "Pendiente" || supervision.estado == "En supervision"
                }
                onResult(Result.success(supervisiones))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun obtenerSupervisionesFinalizadas(onResult: (Result<List<Supervision>>) -> Unit) {
        val supervisorId = firebaseAuth.currentUser?.uid
            ?: run {
                onResult(Result.failure(Exception("No hay un usuario autenticado.")))
                return
            }

        firestore.collection("supervisiones")
            .whereEqualTo("supervisorId", supervisorId)
            .whereEqualTo("estado", "Finalizada")
            .get()
            .addOnSuccessListener { resultado ->
                val supervisiones = resultado.documents.mapNotNull { documento ->
                    val cliente = documento.getString("cliente")
                    val direccion = documento.getString("direccion")
                    val distrito = documento.getString("distrito")
                    val hora = documento.getString("hora")
                    val estado = documento.getString("estado")
                    val latitud = documento.getDouble("latitud")
                    val longitud = documento.getDouble("longitud")

                    if (
                        cliente == null || direccion == null || distrito == null || hora == null ||
                        estado == null || latitud == null || longitud == null
                    ) {
                        null
                    } else {
                        Supervision(
                            id = documento.id,
                            cliente = cliente,
                            direccion = direccion,
                            distrito = distrito,
                            hora = hora,
                            estado = estado,
                            latitud = latitud,
                            longitud = longitud,
                            horaFinalizacion = documento.getTimestamp("horaFinalizacion")
                        )
                    }
                }
                onResult(Result.success(supervisiones))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun registrarLlegada(
        supervisionId: String,
        latitudLlegada: Double,
        longitudLlegada: Double,
        onResult: (Result<Unit>) -> Unit
    ) {
        val datosDeLlegada = mapOf(
            "estado" to "En supervision",
            "horaLlegada" to FieldValue.serverTimestamp(),
            "latitudLlegada" to latitudLlegada,
            "longitudLlegada" to longitudLlegada
        )

        firestore.collection("supervisiones").document(supervisionId)
            .update(datosDeLlegada)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun guardarChecklist(
        supervisionId: String,
        respuestas: Map<String, String>,
        observacionesIncidencias: Map<String, String>,
        observaciones: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val datosChecklist = mapOf(
            "versionChecklist" to 2,
            "limpiezaConservacionCabina" to respuestas["limpiezaConservacionCabina"].orEmpty(),
            "iluminacionCabina" to respuestas["iluminacionCabina"].orEmpty(),
            "botonesPanelControl" to respuestas["botonesPanelControl"].orEmpty(),
            "aperturaCierrePuertas" to respuestas["aperturaCierrePuertas"].orEmpty(),
            "deteccionObstaculosPuertas" to respuestas["deteccionObstaculosPuertas"].orEmpty(),
            "nivelacionPisos" to respuestas["nivelacionPisos"].orEmpty(),
            "ruidosVibraciones" to respuestas["ruidosVibraciones"].orEmpty(),
            "indicadoresPisoDireccion" to respuestas["indicadoresPisoDireccion"].orEmpty(),
            "botonesLlamadaPisos" to respuestas["botonesLlamadaPisos"].orEmpty(),
            "alarmaComunicacionEmergencia" to respuestas["alarmaComunicacionEmergencia"].orEmpty(),
            "senalizacionSeguridad" to respuestas["senalizacionSeguridad"].orEmpty(),
            "accesosLibresObstaculos" to respuestas["accesosLibresObstaculos"].orEmpty(),
            "observacionesIncidencias" to observacionesIncidencias,
            "observaciones" to observaciones,
            "fechaRegistro" to FieldValue.serverTimestamp()
        )

        firestore.collection("supervisiones").document(supervisionId)
            .collection("checklist").document("resultado")
            .set(datosChecklist)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun tieneChecklistGuardado(
        supervisionId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        firestore.collection("supervisiones").document(supervisionId)
            .collection("checklist").document("resultado")
            .get()
            .addOnSuccessListener { documento -> onResult(Result.success(documento.exists())) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun obtenerMovilidad(
        supervisionId: String,
        onResult: (Result<RegistroMovilidad?>) -> Unit
    ) {
        firestore.collection("supervisiones").document(supervisionId)
            .collection("movilidad").document("registro")
            .get()
            .addOnSuccessListener { documento ->
                val movilidad = if (documento.exists()) {
                    documento.toMovilidadRegistro()
                } else {
                    null
                }
                onResult(Result.success(movilidad))
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun observarRequisitosFinalizacion(
        supervisionId: String,
        onResult: (Result<RequisitosFinalizacion>) -> Unit
    ): () -> Unit {
        val supervisionDocument = firestore.collection("supervisiones").document(supervisionId)
        var checklistGuardado: Boolean? = null
        var movilidadGuardada: Boolean? = null

        fun notificarSiEstanVerificados() {
            val checklist = checklistGuardado ?: return
            val movilidad = movilidadGuardada ?: return
            onResult(Result.success(RequisitosFinalizacion(checklist, movilidad)))
        }

        val checklistListener = supervisionDocument
            .collection("checklist").document("resultado")
            .addSnapshotListener { documento, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                checklistGuardado = documento?.exists() == true
                notificarSiEstanVerificados()
            }

        val movilidadListener = supervisionDocument
            .collection("movilidad").document("registro")
            .addSnapshotListener { documento, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                movilidadGuardada = documento?.exists() == true &&
                    movilidadTieneDatosValidos(documento.toMovilidadRegistro())
                notificarSiEstanVerificados()
            }

        return {
            checklistListener.remove()
            movilidadListener.remove()
        }
    }

    fun guardarMovilidad(
        supervisionId: String,
        motivo: String,
        medioTransporte: String,
        especificacionMedioTransporte: String,
        gastoTotal: Double,
        justificacionSinGasto: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val movilidadDocument = firestore.collection("supervisiones").document(supervisionId)
            .collection("movilidad").document("registro")
        val datosMovilidad = mutableMapOf<String, Any>(
            "fechaRegistro" to FieldValue.serverTimestamp(),
            "motivo" to motivo,
            "medioTransporte" to medioTransporte,
            "gastoTotal" to gastoTotal
        )
        if (medioTransporte == "Otro") {
            datosMovilidad["especificacionMedioTransporte"] = especificacionMedioTransporte
        }
        if (gastoTotal == 0.0) {
            datosMovilidad["justificacionSinGasto"] = justificacionSinGasto
        }

        firestore.runTransaction { transaction ->
            if (transaction.get(movilidadDocument).exists()) {
                throw MovilidadYaRegistradaException()
            }
            transaction.set(movilidadDocument, datosMovilidad)
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun finalizarSupervision(
        supervisionId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val supervisionDocument = firestore.collection("supervisiones").document(supervisionId)
        val checklistDocument = supervisionDocument.collection("checklist").document("resultado")
        val movilidadDocument = supervisionDocument.collection("movilidad").document("registro")

        firestore.runTransaction { transaction ->
            val supervision = transaction.get(supervisionDocument)
            val checklist = transaction.get(checklistDocument)
            val movilidad = transaction.get(movilidadDocument)

            if (!supervision.exists()) {
                throw IllegalStateException("No se encontró la supervisión.")
            }
            if (supervision.getString("estado") == "Finalizada") {
                return@runTransaction
            }
            if (!checklist.exists()) {
                throw ChecklistRequeridoException()
            }
            if (!movilidad.exists() || !movilidadTieneDatosValidos(movilidad.toMovilidadRegistro())) {
                throw MovilidadRequeridaException()
            }

            transaction.update(
                supervisionDocument,
                mapOf(
                    "estado" to "Finalizada",
                    "horaFinalizacion" to FieldValue.serverTimestamp()
                )
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun obtenerDetalleHistorial(
        supervisionId: String,
        onResult: (Result<DetalleHistorial>) -> Unit
    ) {
        val supervisionDocument = firestore.collection("supervisiones").document(supervisionId)

        supervisionDocument.get()
            .addOnSuccessListener { supervision ->
                if (!supervision.exists()) {
                    onResult(Result.failure(Exception("No se encontró la supervisión.")))
                    return@addOnSuccessListener
                }

                val horaLlegada = supervision.getTimestamp("horaLlegada")
                val horaFinalizacion = supervision.getTimestamp("horaFinalizacion")

                supervisionDocument.collection("checklist").document("resultado").get()
                    .addOnSuccessListener { checklist ->
                        val resultado = if (checklist.exists()) {
                            ChecklistResult(
                                versionChecklist = checklist.getLong("versionChecklist")?.toInt() ?: 0,
                                limpiezaConservacionCabina = checklist.getString("limpiezaConservacionCabina").orEmpty(),
                                iluminacionCabina = checklist.getString("iluminacionCabina").orEmpty(),
                                botonesPanelControl = checklist.getString("botonesPanelControl").orEmpty(),
                                aperturaCierrePuertas = checklist.getString("aperturaCierrePuertas").orEmpty(),
                                deteccionObstaculosPuertas = checklist.getString("deteccionObstaculosPuertas").orEmpty(),
                                nivelacionPisos = checklist.getString("nivelacionPisos").orEmpty(),
                                ruidosVibraciones = checklist.getString("ruidosVibraciones").orEmpty(),
                                indicadoresPisoDireccion = checklist.getString("indicadoresPisoDireccion").orEmpty(),
                                botonesLlamadaPisos = checklist.getString("botonesLlamadaPisos").orEmpty(),
                                alarmaComunicacionEmergencia = checklist.getString("alarmaComunicacionEmergencia").orEmpty(),
                                senalizacionSeguridad = checklist.getString("senalizacionSeguridad").orEmpty(),
                                accesosLibresObstaculos = checklist.getString("accesosLibresObstaculos").orEmpty(),
                                observacionesIncidencias = checklist.get("observacionesIncidencias").asStringMap(),
                                observaciones = checklist.getString("observaciones").orEmpty(),
                                fechaRegistro = checklist.getTimestamp("fechaRegistro")
                            )
                        } else {
                            null
                        }
                        supervisionDocument.collection("movilidad").document("registro").get()
                            .addOnSuccessListener { movilidadDocument ->
                                val movilidad = if (movilidadDocument.exists()) {
                                    movilidadDocument.toMovilidadRegistro()
                                } else {
                                    null
                                }
                                onResult(
                                    Result.success(
                                        DetalleHistorial(
                                            horaLlegada = horaLlegada,
                                            horaFinalizacion = horaFinalizacion,
                                            checklist = resultado,
                                            movilidad = movilidad
                                        )
                                    )
                                )
                            }
                            .addOnFailureListener { error -> onResult(Result.failure(error)) }
                    }
                    .addOnFailureListener { error -> onResult(Result.failure(error)) }
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }
}

private fun Any?.asStringMap(): Map<String, String> =
    (this as? Map<*, *>)?.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }?.toMap().orEmpty()

private fun com.google.firebase.firestore.DocumentSnapshot.toMovilidadRegistro(): RegistroMovilidad {
    return RegistroMovilidad(
        fechaRegistro = getTimestamp("fechaRegistro"),
        motivo = getString("motivo").orEmpty(),
        medioTransporte = getString("medioTransporte").orEmpty(),
        especificacionMedioTransporte = getString("especificacionMedioTransporte").orEmpty(),
        gastoTotal = getDouble("gastoTotal") ?: Double.NaN,
        justificacionSinGasto = getString("justificacionSinGasto").orEmpty()
    )
}

private fun movilidadTieneDatosValidos(movilidad: RegistroMovilidad): Boolean {
    return movilidad.motivo == "Supervisión de ascensor" &&
        movilidad.medioTransporte in setOf(
            "Transporte público",
            "Taxi/aplicativo",
            "Movilidad propia",
            "Otro"
        ) &&
        (
            (movilidad.medioTransporte == "Otro" &&
                movilidad.especificacionMedioTransporte.isNotBlank()) ||
                (movilidad.medioTransporte != "Otro" &&
                    movilidad.especificacionMedioTransporte.isBlank())
            ) &&
        movilidad.gastoTotal.isFinite() &&
        movilidad.gastoTotal >= 0.0 &&
        movilidad.gastoTotal <= 1_000_000.0 &&
        (
            (movilidad.gastoTotal == 0.0 && movilidad.justificacionSinGasto.isNotBlank()) ||
                (movilidad.gastoTotal > 0.0 && movilidad.justificacionSinGasto.isBlank())
            )
}
