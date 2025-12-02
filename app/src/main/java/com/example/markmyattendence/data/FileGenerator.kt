package com.example.markmyattendence.data

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.markmyattendence.data.StudentAttendance
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileGenerator {

    // NOTE: This implementation requires the WRITE_EXTERNAL_STORAGE permission on older Android versions,
    // or proper use of the MediaStore API for modern versions.

    fun generateCsv(
        context: Context,
        className: String,
        date: String,
        attendanceList: List<StudentAttendance>
    ) {
        if (attendanceList.isEmpty()) {
            Toast.makeText(context, "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use a safe file name
        val safeClassName = className.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${safeClassName}_Attendance_${date}.csv"

        // Target the public Downloads folder
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)

        try {
            val writer = FileWriter(file)

            // Write Header
            writer.append("Class Name: $className\n")
            writer.append("Date: $date\n\n")
            writer.append("Student Name,Attendance Status,UID\n")

            // Write Data Rows
            attendanceList.forEach { student ->
                val status = if (student.isPresent) "PRESENT" else "ABSENT"
                writer.append("${student.studentName},${status},${student.studentUid}\n")
            }

            writer.flush()
            writer.close()

            Toast.makeText(context, "CSV saved successfully to Downloads/$fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error saving CSV file: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * Placeholder for full PDF generation logic.
     */
    fun generatePdf(
        context: Context,
        className: String,
        date: String,
        attendanceList: List<StudentAttendance>
    ) {
        // You would implement PdfDocument or a library here.
        Toast.makeText(context, "PDF generation triggered (Not fully implemented).", Toast.LENGTH_SHORT).show()
    }
}