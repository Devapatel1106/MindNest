package com.example.mindnest.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.mindnest.R
import androidx.core.content.ContextCompat
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateWellnessReport(
        context: Context,
        userName: String,
        mindScore: Int,
        avgScore: Int,
        highScore: Int,
        lowScore: Int,
        insight: String,
        chartBitmap: Bitmap?,
        moduleData: Map<String, String>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // App Colors
        val primaryColor = ContextCompat.getColor(context, R.color.lavender_primary)
        val darkColor = ContextCompat.getColor(context, R.color.lavender_dark)
        val textColor = ContextCompat.getColor(context, R.color.text_primary)
        val lightColor = ContextCompat.getColor(context, R.color.Light)

        var yPos = 40f

        // Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f // Reduced from 20f
        paint.color = primaryColor
        canvas.drawText("MindNest Weekly Wellness Report", 50f, yPos, paint)
        yPos += 25f

        // Date and User
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f // Reduced from 10f
        paint.color = textColor
        val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr", 50f, yPos, paint)
        yPos += 12f
        canvas.drawText("User: $userName", 50f, yPos, paint)
        yPos += 20f

        // Horizontal Line
        paint.strokeWidth = 0.8f
        paint.color = lightColor
        canvas.drawLine(50f, yPos, 545f, yPos, paint)
        yPos += 20f

        // Performance Summary Section
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f // Reduced from 14f
        paint.color = darkColor
        canvas.drawText("Overall Performance", 50f, yPos, paint)
        yPos += 15f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f // Reduced from 10f
        paint.color = textColor
        canvas.drawText("Current Mind Score: $mindScore/100", 70f, yPos, paint)
        yPos += 12f
        canvas.drawText("Weekly Average: $avgScore", 70f, yPos, paint)
        yPos += 12f
        canvas.drawText("Weekly High: $highScore", 70f, yPos, paint)
        yPos += 12f
        canvas.drawText("Weekly Low: $lowScore", 70f, yPos, paint)
        yPos += 20f

        // Weekly Insight
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = darkColor
        paint.textSize = 12f // Reduced from 14f
        canvas.drawText("AI Insight", 50f, yPos, paint)
        yPos += 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 9f // Reduced from 10f
        paint.color = textColor
        val wrappedInsight = wrapText(insight, 480f, paint)
        wrappedInsight.forEach { line ->
            canvas.drawText(line, 60f, yPos, paint)
            yPos += 12f
        }
        yPos += 18f

        // Module Specific Data Section
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = darkColor
        paint.textSize = 12f // Reduced from 14f
        canvas.drawText("Module Summaries (Today)", 50f, yPos, paint)
        yPos += 15f

        paint.textSize = 9f // Reduced from 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textColor

        val leftColX = 70f
        val rightColX = 300f
        var currentModuleY = yPos

        val modules = moduleData.toList()
        for (i in modules.indices) {
            val x = if (i % 2 == 0) leftColX else rightColX
            val (title, data) = modules[i]

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("$title:", x, currentModuleY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val dataX = x + paint.measureText("$title: ")
            canvas.drawText(data, dataX, currentModuleY, paint)

            if (i % 2 != 0 || i == modules.size - 1) {
                currentModuleY += 15f
            }
        }
        yPos = currentModuleY + 10f

        // Chart Section
        if (chartBitmap != null) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = darkColor
            paint.textSize = 12f // Reduced from 14f
            canvas.drawText("7-Day Mind Score Trend", 50f, yPos, paint)
            yPos += 12f

            // Further decreased graph size (reduced to 250f width)
            val scaledWidth = 250f
            val scaledHeight = (chartBitmap.height.toFloat() / chartBitmap.width.toFloat()) * scaledWidth

            val destRect = RectF(50f, yPos, 50f + scaledWidth, yPos + scaledHeight)
            canvas.drawBitmap(chartBitmap, null, destRect, null)
        }

        // Footer
        paint.textSize = 7f // Reduced from 8f
        paint.color = Color.GRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("MindNest - Your Mental Sanctuary | Stay Mindful, Stay Healthy", 50f, 820f, paint)
        canvas.drawText("Page 1 of 1", 520f, 820f, paint)

        pdfDocument.finishPage(page)

        val fileName = "MindNest_Report_${SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())}.pdf"
        val uri = savePdfToMediaStore(context, pdfDocument, fileName)

        pdfDocument.close()
        return uri
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun savePdfToMediaStore(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        }
        return uri
    }
}