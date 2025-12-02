package com.example.markmyattendence.data

import android.content.Context
import android.widget.Toast
import android.graphics.*
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.content.ContentValues
import android.provider.MediaStore
import java.io.IOException

object FileGenerator {

    fun generatePdf(
        context: Context,
        className: String,
        date: String,
        attendanceList: List<StudentAttendance>
    ) {
        // We use a final try/catch block to ensure any failure is reported.
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()

            val pageWidth = 595  // A4 width
            val pageHeight = 842 // A4 height

            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                textSize = 22f
                color = Color.BLACK
            }

            val normalPaint = Paint().apply {
                textSize = 14f
                color = Color.BLACK
            }

            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1.5f
            }

            var y = 80f
            var pageNumber = 1

            // Helper function to create new pages for multi-page reports
            fun newPage(): android.graphics.pdf.PdfDocument.Page {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNumber
                ).create()
                pageNumber++
                return pdfDocument.startPage(pageInfo)
            }

            var page = newPage()
            var canvas = page.canvas

            // ---------- PAGE HEADER (Class and Date at the top) ----------
            canvas.drawText("Attendance Report", 200f, y, titlePaint)
            y += 30

            canvas.drawText("Class: $className", 30f, y, normalPaint)
            y += 20
            canvas.drawText("Date: $date", 30f, y, normalPaint)
            y += 40

            // ---------- TABLE HEADER ----------
            canvas.drawLine(20f, y, pageWidth - 20f, y, linePaint)
            y += 20
            canvas.drawText("Name", 30f, y, titlePaint)
            canvas.drawText("UID", 230f, y, titlePaint)
            canvas.drawText("Status", 400f, y, titlePaint)
            y += 10
            canvas.drawLine(20f, y, pageWidth - 20f, y, linePaint)
            y += 25

            // ---------- TABLE ROWS ----------
            attendanceList.forEach { student ->

                if (y > 780) {      // End of page → create new page
                    pdfDocument.finishPage(page)
                    y = 80f
                    page = newPage()
                    canvas = page.canvas

                    canvas.drawText("Attendance Report (cont.)", 150f, y, titlePaint)
                    y += 40
                }

                canvas.drawText(student.studentName, 30f, y, normalPaint)
                canvas.drawText(student.studentUid, 230f, y, normalPaint)

                val status = if (student.isPresent) "Present" else "Absent"
                canvas.drawText(status, 400f, y, normalPaint)

                y += 25
            }

            pdfDocument.finishPage(page)


            val fileName = "Attendance_${className}_${date}.pdf"

            // 1. Prepare file details for MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    // Open an output stream to the Uri
                    resolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            // 1. Write the PDF content to the stream
                            pdfDocument.writeTo(outputStream)

                            // 🔥 FIX: Flush the stream to ensure all data is written immediately
                            outputStream.flush()

                            Toast.makeText(context, "PDF saved to Public Downloads folder!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error: Could not open output stream.", Toast.LENGTH_LONG).show()
                        }
                    } // The .use{} block automatically closes the outputStream
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    Toast.makeText(context, "File Write Error: ${ioe.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Error: Failed to create file entry in MediaStore.", Toast.LENGTH_LONG).show()
            }

            // 🔥 FIX: Close the PdfDocument ONLY after writing is fully complete and successful
            pdfDocument.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Final Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}