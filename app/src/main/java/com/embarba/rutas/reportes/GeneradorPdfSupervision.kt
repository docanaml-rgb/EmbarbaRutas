package com.embarba.rutas.reportes

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.embarba.rutas.R
import com.embarba.rutas.modelos.ChecklistResult
import com.embarba.rutas.modelos.DetalleHistorial
import com.embarba.rutas.modelos.RegistroMovilidad
import com.embarba.rutas.modelos.Supervision
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PdfReportException(message: String) : IllegalStateException(message)

object GeneradorPdfSupervision {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val REPORT_TITLE = "INFORME DE SUPERVISIÓN DE ASCENSORES"
    private val peruTimeZone = TimeZone.getTimeZone("America/Lima")

    fun isValidChecklist(checklist: ChecklistResult?): Boolean {
        if (checklist == null || checklist.versionChecklist != 2 || checklist.fechaRegistro == null) {
            return false
        }
        return checklistItems(checklist).all { item ->
            item.answer in setOf("Cumple", "No cumple") &&
                (item.answer != "No cumple" || item.observation.isNotBlank())
        }
    }

    fun generate(
        context: Context,
        supervision: Supervision,
        detail: DetalleHistorial,
        supervisorName: String
    ): File {
        validateReportData(supervision, detail, supervisorName)
        val checklist = requireNotNull(detail.checklist)
        val movilidad = requireNotNull(detail.movilidad)
        val reportDirectory = File(context.cacheDir, "reports").apply { mkdirs() }
        if (!reportDirectory.isDirectory) {
            throw PdfReportException("No se pudo preparar el almacenamiento temporal del informe.")
        }
        val safeCode = supervision.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outputFile = File(reportDirectory, "Informe_$safeCode.pdf")
        val document = PdfDocument()

        try {
            val writer = ReportWriter(document, supervision.id)
            writer.startPage()
            writer.drawMainHeader(context)
            writer.drawSectionTitle("DATOS DE LA SUPERVISIÓN")
            writer.drawInfoRows(
                listOf(
                    "Código de supervisión" to supervision.id,
                    "Cliente" to supervision.cliente,
                    "Dirección" to supervision.direccion,
                    "Distrito" to supervision.distrito,
                    "Supervisor" to supervisorName,
                    "Llegada" to formatTimestamp(requireNotNull(detail.horaLlegada)),
                    "Finalización" to formatTimestamp(requireNotNull(detail.horaFinalizacion))
                )
            )

            writer.drawSectionTitle("VERIFICACIONES DEL CHECKLIST")
            checklistItems(checklist).forEachIndexed { index, item ->
                writer.drawChecklistItem(index + 1, item)
            }

            if (checklist.observaciones.isNotBlank()) {
                writer.drawSectionTitle("OBSERVACIONES GENERALES")
                writer.drawTextBlock(checklist.observaciones.trim())
            }

            writer.drawSectionTitle("MOVILIDAD")
            val mobilityRows = buildList {
                add("Fecha de registro" to formatTimestamp(requireNotNull(movilidad.fechaRegistro)))
                add("Motivo" to movilidad.motivo)
                add("Medio de transporte" to movilidad.medioTransporte)
                if (movilidad.medioTransporte == "Otro") {
                    add("Especificación" to movilidad.especificacionMedioTransporte)
                }
                add("Gasto total" to String.format(Locale.US, "S/ %.2f", movilidad.gastoTotal))
                if (movilidad.gastoTotal == 0.0) {
                    add("Justificación sin gasto" to movilidad.justificacionSinGasto)
                }
            }
            writer.drawInfoRows(mobilityRows)
            writer.finish()

            FileOutputStream(outputFile).use(document::writeTo)
            return outputFile
        } catch (error: Exception) {
            outputFile.delete()
            if (error is PdfReportException) throw error
            throw PdfReportException("No se pudo generar el informe PDF.")
        } finally {
            document.close()
        }
    }

    private fun validateReportData(
        supervision: Supervision,
        detail: DetalleHistorial,
        supervisorName: String
    ) {
        if (supervision.estado != "Finalizada") {
            throw PdfReportException("La supervisión debe estar finalizada para generar el informe.")
        }
        if (supervision.id.isBlank() || supervision.cliente.isBlank() ||
            supervision.direccion.isBlank() || supervision.distrito.isBlank()
        ) {
            throw PdfReportException("Faltan datos de identificación de la supervisión.")
        }
        if (supervisorName.isBlank()) {
            throw PdfReportException("No se encontró el nombre del supervisor en su perfil.")
        }
        if (detail.horaLlegada == null || detail.horaFinalizacion == null) {
            throw PdfReportException("Faltan las fechas de llegada o finalización.")
        }
        if (!isValidChecklist(detail.checklist)) {
            throw PdfReportException("El checklist guardado no es válido para generar el informe.")
        }
        if (!isValidMobility(detail.movilidad)) {
            throw PdfReportException("Faltan datos válidos de movilidad para generar el informe.")
        }
    }

    private fun isValidMobility(movilidad: RegistroMovilidad?): Boolean {
        if (movilidad == null || movilidad.fechaRegistro == null) return false
        if (movilidad.motivo.isBlank() || movilidad.medioTransporte !in setOf(
                "Transporte público",
                "Taxi/aplicativo",
                "Movilidad propia",
                "Otro"
            )
        ) return false
        if (movilidad.medioTransporte == "Otro" &&
            movilidad.especificacionMedioTransporte.isBlank()
        ) return false
        if (!movilidad.gastoTotal.isFinite() || movilidad.gastoTotal < 0.0) return false
        return movilidad.gastoTotal != 0.0 || movilidad.justificacionSinGasto.isNotBlank()
    }

    private fun checklistItems(checklist: ChecklistResult): List<ChecklistItem> = listOf(
        ChecklistItem(
            "Limpieza y conservación de la cabina",
            checklist.limpiezaConservacionCabina,
            checklist.observacionesIncidencias["limpiezaConservacionCabina"].orEmpty()
        ),
        ChecklistItem(
            "Iluminación interior de la cabina",
            checklist.iluminacionCabina,
            checklist.observacionesIncidencias["iluminacionCabina"].orEmpty()
        ),
        ChecklistItem(
            "Botones del panel de control",
            checklist.botonesPanelControl,
            checklist.observacionesIncidencias["botonesPanelControl"].orEmpty()
        ),
        ChecklistItem(
            "Apertura y cierre de puertas",
            checklist.aperturaCierrePuertas,
            checklist.observacionesIncidencias["aperturaCierrePuertas"].orEmpty()
        ),
        ChecklistItem(
            "Detección de obstáculos",
            checklist.deteccionObstaculosPuertas,
            checklist.observacionesIncidencias["deteccionObstaculosPuertas"].orEmpty()
        ),
        ChecklistItem(
            "Nivelación en cada piso",
            checklist.nivelacionPisos,
            checklist.observacionesIncidencias["nivelacionPisos"].orEmpty()
        ),
        ChecklistItem(
            "Ausencia de ruidos o vibraciones anormales",
            checklist.ruidosVibraciones,
            checklist.observacionesIncidencias["ruidosVibraciones"].orEmpty()
        ),
        ChecklistItem(
            "Indicadores de piso y dirección",
            checklist.indicadoresPisoDireccion,
            checklist.observacionesIncidencias["indicadoresPisoDireccion"].orEmpty()
        ),
        ChecklistItem(
            "Botones de llamada en los pisos",
            checklist.botonesLlamadaPisos,
            checklist.observacionesIncidencias["botonesLlamadaPisos"].orEmpty()
        ),
        ChecklistItem(
            "Alarma o comunicación de emergencia",
            checklist.alarmaComunicacionEmergencia,
            checklist.observacionesIncidencias["alarmaComunicacionEmergencia"].orEmpty()
        ),
        ChecklistItem(
            "Señalización e instrucciones de seguridad",
            checklist.senalizacionSeguridad,
            checklist.observacionesIncidencias["senalizacionSeguridad"].orEmpty()
        ),
        ChecklistItem(
            "Accesos libres de obstáculos",
            checklist.accesosLibresObstaculos,
            checklist.observacionesIncidencias["accesosLibresObstaculos"].orEmpty()
        )
    )

    private fun formatTimestamp(timestamp: Timestamp): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
            timeZone = peruTimeZone
        }.format(timestamp.toDate())

    private data class ChecklistItem(
        val question: String,
        val answer: String,
        val observation: String
    )

    private class ReportWriter(
        private val document: PdfDocument,
        private val supervisionCode: String
    ) {
        private val darkBlue = Color.rgb(15, 55, 94)
        private val corporateBlue = Color.rgb(24, 91, 157)
        private val lightBlue = Color.rgb(231, 241, 250)
        private val lightGray = Color.rgb(246, 248, 250)
        private val textColor = Color.rgb(35, 43, 51)
        private val mutedColor = Color.rgb(91, 101, 111)
        private val successColor = Color.rgb(35, 121, 76)
        private val errorColor = Color.rgb(177, 49, 49)
        private val margin = 42f
        private val contentWidth = PAGE_WIDTH - margin * 2
        private val contentBottom = PAGE_HEIGHT - 42f
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var pageNumber = 0
        private var y = margin

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = textColor
        }

        fun startPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = requireNotNull(page).canvas
            y = margin
            if (pageNumber > 1) drawContinuationHeader()
        }

        fun finish() = finishCurrentPage()

        fun drawMainHeader(context: Context) {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logoembarba)
                ?: throw PdfReportException("No se pudo cargar el logotipo del informe.")
            val maxLogoWidth = 125f
            val maxLogoHeight = 52f
            val scale = minOf(maxLogoWidth / logo.width, maxLogoHeight / logo.height)
            val logoWidth = logo.width * scale
            val logoHeight = logo.height * scale
            canvas.drawBitmap(
                logo,
                null,
                RectF(margin, y, margin + logoWidth, y + logoHeight),
                paint
            )
            val titleX = margin + maxLogoWidth + 20f
            val titleWidth = PAGE_WIDTH - margin - titleX
            paint.applyText(17f, darkBlue, true)
            val titleLines = wrapText(REPORT_TITLE, titleWidth, paint)
            var titleY = y + 16f
            titleLines.forEach { line ->
                canvas.drawText(line, titleX, titleY, paint)
                titleY += 20f
            }
            y += maxOf(logoHeight, titleLines.size * 20f) + 16f
            paint.color = corporateBlue
            paint.strokeWidth = 2f
            canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, paint)
            y += 18f
            logo.recycle()
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(34f)
            paint.applyText(11f, Color.WHITE, true)
            canvas.drawRoundRect(
                RectF(margin, y, PAGE_WIDTH - margin, y + 24f),
                4f,
                4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = corporateBlue }
            )
            canvas.drawText(title, margin + 10f, y + 16f, paint)
            y += 34f
        }

        fun drawInfoRows(rows: List<Pair<String, String>>) {
            rows.forEach { (label, value) ->
                val labelWidth = 135f
                paint.applyText(9f, mutedColor, true)
                val labelLines = wrapText(label, labelWidth - 8f, paint)
                paint.applyText(9.5f, textColor, false)
                val valueLines = wrapText(value, contentWidth - labelWidth - 16f, paint)
                val lineCount = maxOf(labelLines.size, valueLines.size)
                val rowHeight = maxOf(27f, lineCount * 12f + 12f)
                ensureSpace(rowHeight)
                canvas.drawRoundRect(
                    RectF(margin, y, PAGE_WIDTH - margin, y + rowHeight - 3f),
                    3f,
                    3f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lightGray }
                )
                paint.applyText(9f, mutedColor, true)
                labelLines.forEachIndexed { index, line ->
                    canvas.drawText(line, margin + 8f, y + 14f + index * 12f, paint)
                }
                paint.applyText(9.5f, textColor, false)
                valueLines.forEachIndexed { index, line ->
                    canvas.drawText(line, margin + labelWidth, y + 14f + index * 12f, paint)
                }
                y += rowHeight
            }
            y += 6f
        }

        fun drawChecklistItem(number: Int, item: ChecklistItem) {
            paint.applyText(10f, textColor, true)
            val questionLines = wrapText("$number. ${item.question}", contentWidth - 24f, paint)
            paint.applyText(9.5f, textColor, false)
            val observationLines = if (item.answer == "No cumple") {
                wrapText("Observación: ${item.observation.trim()}", contentWidth - 24f, paint)
            } else {
                emptyList()
            }
            val blockHeight = 18f + questionLines.size * 13f + 16f +
                observationLines.size * 12f + if (observationLines.isEmpty()) 4f else 10f
            ensureSpace(blockHeight)
            canvas.drawRoundRect(
                RectF(margin, y, PAGE_WIDTH - margin, y + blockHeight - 6f),
                5f,
                5f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lightGray }
            )
            val statusColor = if (item.answer == "Cumple") successColor else errorColor
            canvas.drawRoundRect(
                RectF(margin, y, margin + 4f, y + blockHeight - 6f),
                2f,
                2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor }
            )
            var lineY = y + 17f
            paint.applyText(10f, textColor, true)
            questionLines.forEach { line ->
                canvas.drawText(line, margin + 12f, lineY, paint)
                lineY += 13f
            }
            paint.applyText(9.5f, statusColor, true)
            canvas.drawText(item.answer, margin + 12f, lineY + 3f, paint)
            lineY += 18f
            if (observationLines.isNotEmpty()) {
                paint.applyText(9f, textColor, false)
                observationLines.forEach { line ->
                    canvas.drawText(line, margin + 12f, lineY, paint)
                    lineY += 12f
                }
            }
            y += blockHeight
        }

        fun drawTextBlock(text: String) {
            paint.applyText(9.5f, textColor, false)
            val lines = wrapText(text, contentWidth - 20f, paint)
            val blockHeight = lines.size * 13f + 18f
            ensureSpace(blockHeight)
            canvas.drawRoundRect(
                RectF(margin, y, PAGE_WIDTH - margin, y + blockHeight),
                4f,
                4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lightBlue }
            )
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, margin + 10f, y + 14f + index * 13f, paint)
            }
            y += blockHeight + 8f
        }

        private fun drawContinuationHeader() {
            paint.applyText(9f, darkBlue, true)
            canvas.drawText("EMBARBA ELEVADORES", margin, y, paint)
            paint.applyText(8.5f, mutedColor, false)
            val codeText = "Informe $supervisionCode"
            canvas.drawText(codeText, PAGE_WIDTH - margin - paint.measureText(codeText), y, paint)
            y += 10f
            paint.color = corporateBlue
            paint.strokeWidth = 1f
            canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, paint)
            y += 18f
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > contentBottom) startPage()
        }

        private fun finishCurrentPage() {
            val currentPage = page ?: return
            paint.color = Color.rgb(190, 198, 205)
            paint.strokeWidth = 0.7f
            canvas.drawLine(margin, PAGE_HEIGHT - 30f, PAGE_WIDTH - margin, PAGE_HEIGHT - 30f, paint)
            paint.applyText(8f, mutedColor, false)
            canvas.drawText("Embarba Elevadores", margin, PAGE_HEIGHT - 17f, paint)
            val pageText = "Página $pageNumber"
            canvas.drawText(
                pageText,
                PAGE_WIDTH - margin - paint.measureText(pageText),
                PAGE_HEIGHT - 17f,
                paint
            )
            document.finishPage(currentPage)
            page = null
        }

        private fun Paint.applyText(size: Float, colorValue: Int, bold: Boolean) {
            textSize = size
            color = colorValue
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            style = Paint.Style.FILL
        }

        private fun wrapText(text: String, maxWidth: Float, textPaint: Paint): List<String> {
            if (text.isBlank()) return listOf("")
            return text.lineSequence().flatMap { paragraph ->
                val remainingLines = mutableListOf<String>()
                var remaining = paragraph.trim()
                while (remaining.isNotEmpty()) {
                    var count = textPaint.breakText(remaining, true, maxWidth, null)
                    if (count <= 0) count = 1
                    if (count < remaining.length) {
                        val lastSpace = remaining.substring(0, count).lastIndexOf(' ')
                        if (lastSpace > 0) count = lastSpace
                    }
                    remainingLines += remaining.substring(0, count).trim()
                    remaining = remaining.substring(count).trimStart()
                }
                remainingLines.ifEmpty { listOf("") }.asSequence()
            }.toList()
        }
    }
}
