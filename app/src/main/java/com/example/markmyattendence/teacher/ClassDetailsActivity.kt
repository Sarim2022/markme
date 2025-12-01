package com.example.markmyattendence.teacher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ActiveAttendanceSession
import com.example.markmyattendence.data.AttendanceRecord
import com.example.markmyattendence.data.EnrolledStudent
import com.example.markmyattendence.teacher.Adapter.EnrolledStudentsAdapter
import com.example.markmyattendence.databinding.ActivityClassDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.util.Log
import android.view.View
import java.util.UUID
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.recyclerview.widget.LinearLayoutManager

class ClassDetailsActivity : AppCompatActivity() {

    private val TAG = "ClassDetailsActivity"
    private lateinit var binding: ActivityClassDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var activeAttendanceRecordId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private var currentSessionRecordId: String? = null
    private lateinit var enrolledStudentsAdapter: EnrolledStudentsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.backbutton.setOnClickListener {
            finish()
        }
        val classId = intent.getStringExtra("CLASS_ID")
        binding.fabStartAttendance.setOnClickListener {
            Toast.makeText(this, "Attendence start now !", Toast.LENGTH_SHORT).show()
            startAttendanceSession(classId.toString())
        }

        // Set up end attendance button in QR card
        binding.fabEndAttendance.setOnClickListener {
            endAttendanceSession(classId.toString())
        }


        val className = intent.getStringExtra("CLASS_NAME")
        val classroom = intent.getStringExtra("CLASS_ROOM")
        val startTime = intent.getStringExtra("START_TIME")
        val endTime = intent.getStringExtra("END_TIME")
        val classCode = intent.getStringExtra("CLASS_CODE")
        val autoApprove = intent.getBooleanExtra("AUTO_APPROVE", false)
        val startDate = intent.getStringExtra("START_DATE")
        val repeatDays = intent.getStringArrayListExtra("REPEAT_DAYS")

        binding.tvHeaderInfo.text = "Class Details ${className}"

        val maxStudentsCount = intent.getIntExtra("MAX_STUDENTS", 0)



        // Populate the main summary card
        if (className != null && classId != null) {

            // 3.1 Class Name and Subject
            // Assuming 'className' holds the subject name (e.g., "maths")
            // and 'classroom' or 'department' could provide context
            binding.tvDetailClassName.text = "$className ($classroom)"

            // 3.2 Time
            binding.tvDetailTime.text = "$startTime - $endTime"

            // 3.3 Class Code
            binding.tvDetailClassCode.text = classCode

            // 3.4 Students Count (This usually comes from the list of ENROLLED students,
            // but we'll use maxStudents for now as a placeholder)
            if (maxStudentsCount > 0) {
                binding.tvDetailStudentsCount.text = "$maxStudentsCount Students"
            } else {
                binding.tvDetailStudentsCount.text = "Count N/A" // Or "No Students Set"
            }

            // 3.5 Approval Status
            val approvalStatus = if (autoApprove) "Automatic" else "Manual"
            binding.tvDetailAutoApprove.text = approvalStatus

            // 3.6 Schedule (Days and Start Date)
            val daysString = repeatDays?.joinToString(", ") ?: "N/A"
            binding.tvDetailSchedule.text = "$daysString, Starting $startDate"

            // 3.7 Start Attendance FAB/Button is already set up above

            // Setup delete button click listener
            binding.fabDeleteDelete.setOnClickListener{
                Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show()
                showDeleteConfirmationDialog(classId, className)
            }

            // Initialize RecyclerView for enrolled students
            setupEnrolledStudentsRecyclerView()

            loadEnrolledStudents(classId)

        } else {
            // Handle case where required data (ID/Name) is missing
            Log.e(TAG, "Error: CLASS_ID or CLASS_NAME not found in Intent.")
            // You might want to show a message to the user and close the activity
            // finish()
        }
    }


    fun generateSessionData(classId: String, teacherUid: String): ActiveAttendanceSession {
        val qrToken = UUID.randomUUID().toString() // Generates a unique secure token
        val startTime = Timestamp.now()

        // Set expiry to 5 minutes from now (adjust as needed)
        val expiryDate = Date(startTime.toDate().time + TimeUnit.MINUTES.toMillis(5))
        val expiryTime = Timestamp(expiryDate)

        return ActiveAttendanceSession(
            classId = classId,
            qrCodeToken = qrToken,
            startTime = startTime,
            expiryTime = expiryTime,
            teacherUid = teacherUid
        )
    }
    fun startAttendanceSession(classId: String) {

        // 1. Get the currently logged-in teacher's UID from Firebase Auth
        val teacherUid: String = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            // Handle the case where the user is not logged in
            // In a real app, this should force a logout or show a login screen.
            Toast.makeText(this, "Error: Teacher not logged in.", Toast.LENGTH_LONG).show()
            return // Exit the function
        }

        val db = FirebaseFirestore.getInstance()

        // 2. Generate the temporary session object
        // This helper function creates the secure token, start time, and expiry time.
        val newSession = generateSessionData(classId, teacherUid)

        // A. Write the session object to the 'ActiveAttendanceSessions' collection
        db.collection("ActiveAttendanceSessions")
            .document(classId) // Use the Class ID as the document ID for easy retrieval
            .set(newSession)
            .addOnSuccessListener {
                // SUCCESS: The temporary session is active! Now create the permanent record shell.

                // B. Prepare the unique ID for the 'AttendanceRecords' document
                // Format: [classId]_[yyyyMMdd]_[HHmm]
                val recordDateTimeFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                val recordId = "${classId}_${recordDateTimeFormat.format(newSession.startTime.toDate())}"

                // Store the record ID for later use when ending the session
                currentSessionRecordId = recordId

                // C. Create the initial 'AttendanceRecords' document
                val initialRecord = AttendanceRecord(
                    classId = classId,
                    date = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).format(newSession.startTime.toDate()),
                    sessionStart = newSession.startTime,
                    teacherUid = newSession.teacherUid
                )

                db.collection("AttendanceRecords")
                    .document(recordId) // Use the combined unique ID
                    .set(initialRecord)
                    .addOnSuccessListener {
                        // ALL SUCCESS: Both necessary documents are created.

                        // D. Generate and display the QR Code to the students
                        val qrCodeData = newSession.qrCodeToken

                        // Display the QR code
                        displayQrCode(qrCodeData)

                        Toast.makeText(this, "Attendance started. Valid for 5 minutes!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        // Failure to create the permanent Attendance Record
                        Toast.makeText(this, "Error creating record: ${e.message}", Toast.LENGTH_LONG).show()
                        // OPTIONAL: Delete the temporary session created in step A to clean up.
                    }

            }
            .addOnFailureListener { e ->
                // Failure to create the temporary Active Session
                Toast.makeText(this, "Error starting attendance: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    // Function to generate a Bitmap from a string
    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap? {
        try {
            val writer = QRCodeWriter()
            // Encode the string content into a BitMatrix (the pattern of the QR code)
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

            val width = bitMatrix.width
            val height = bitMatrix.height
            // Create a blank Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            // Loop through the BitMatrix and set the pixels in the Bitmap
            for (x in 0 until width) {
                for (y in 0 until height) {
                    // If bitMatrix.get(x, y) is true (black square), set pixel to black, else white.
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            // Log the error if encoding fails
            Log.e("QR_CODE_GENERATOR", "Error generating QR Code: ${e.message}")
            return null
        }
    }
    private fun displayQrCode(data: String) {

        // 1. Generate the Bitmap using the helper function
        val qrBitmap = generateQrCodeBitmap(data)

        if (qrBitmap != null) {
            // 2. Set the generated Bitmap to the ImageView
            binding.qrCodeImageView.setImageBitmap(qrBitmap)

            // 3. Make the QR code card visible and hide the start button
            binding.cardQrCodeSession.visibility = View.VISIBLE
            binding.fabStartAttendance.visibility = View.GONE

            // Show the message on successful display
            Toast.makeText(this, "QR Code is now active.", Toast.LENGTH_SHORT).show()

        } else {
            // Handle the error if generation failed
            Toast.makeText(this, "Failed to generate QR Code image. Check logs.", Toast.LENGTH_LONG).show()
            binding.qrCodeImageView.visibility = View.GONE
        }
    }
    private fun setupEnrolledStudentsRecyclerView() {
        enrolledStudentsAdapter = EnrolledStudentsAdapter()
        binding.recyclerViewStudents.apply {
            layoutManager = LinearLayoutManager(this@ClassDetailsActivity)
            adapter = enrolledStudentsAdapter
        }
    }

    private fun loadEnrolledStudents(classId: String) {
        Log.d(TAG, "Starting student data load for Class ID: $classId")

        // Step 1: Fetch the class document to get enrolled student UIDs
        db.collection("classes")
            .document(classId)
            .get()
            .addOnSuccessListener { classDocument ->
                if (!classDocument.exists()) {
                    Log.e(TAG, "Class document not found for ID: $classId")
                    enrolledStudentsAdapter.updateStudents(emptyList())
                    return@addOnSuccessListener
                }

                // Extract enrolled student UIDs
                @Suppress("UNCHECKED_CAST")
                val enrolledStudentUids = classDocument.get("studentJoined") as? List<String> ?: emptyList()

                if (enrolledStudentUids.isEmpty()) {
                    Log.d(TAG, "No enrolled students found for class: $classId")
                    enrolledStudentsAdapter.updateStudents(emptyList())
                    return@addOnSuccessListener
                }

                // Step 2: Fetch student profiles in batches (Firestore limit is 10 for whereIn)
                fetchStudentProfiles(enrolledStudentUids)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching class document: $e")
                Toast.makeText(this, "Failed to load enrolled students", Toast.LENGTH_SHORT).show()
                enrolledStudentsAdapter.updateStudents(emptyList())
            }
    }

    private fun fetchStudentProfiles(studentUids: List<String>) {
        val enrolledStudents = mutableListOf<EnrolledStudent>()

        // Split into batches of 10 (Firestore whereIn limit)
        val batches = studentUids.chunked(10)

        // Use a counter to track when all batches are complete
        var completedBatches = 0

        for (batch in batches) {
            db.collection("users")
                .whereIn("uid", batch)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val student = EnrolledStudent(
                            uid = document.getString("uid") ?: "",
                            name = document.getString("name") ?: "Unknown",
                            email = document.getString("email") ?: "",
                            studentId = document.getString("studentId") ?: ""
                        )
                        enrolledStudents.add(student)
                    }

                    // Check if all batches are complete
                    completedBatches++
                    if (completedBatches == batches.size) {
                        // All batches complete, update the adapter
                        enrolledStudentsAdapter.updateStudents(enrolledStudents.sortedBy { it.name })
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching student profiles batch: $e")
                    completedBatches++
                    if (completedBatches == batches.size) {
                        enrolledStudentsAdapter.updateStudents(enrolledStudents.sortedBy { it.name })
                    }
                }
        }
    }

    private fun showDeleteConfirmationDialog(classId: String?, className: String?) {
        if (classId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete \"$className\"? This action cannot be undone and will remove all associated data.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteClassFromFirestore(classId, className)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteClassFromFirestore(classId: String, className: String?) {
        val currentTeacherUid = auth.currentUser?.uid

        if (currentTeacherUid.isNullOrEmpty()) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val teacherRef = db.collection("users").document(currentTeacherUid)

        // Use batch write to ensure atomicity
        db.runBatch { batch ->
            // Remove the class document
            val classRef = db.collection("classes").document(classId)
            batch.delete(classRef)

            // Remove the classId from teacher's myClasses array
            batch.update(teacherRef, "myClasses", FieldValue.arrayRemove(classId))
        }
        .addOnSuccessListener {
            Toast.makeText(this, "\"$className\" deleted successfully!", Toast.LENGTH_SHORT).show()

            // Navigate back to TeacherHomeActivity
            val intent = Intent(this, TeacherHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error deleting class: $classId", e)
            Toast.makeText(this, "Error deleting class: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun endAttendanceSession(classId: String) {

        // Check if we have an active session record ID
        val activeRecordId = currentSessionRecordId
        if (activeRecordId.isNullOrEmpty()) {
            Toast.makeText(this, "No active attendance session to end.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Update the permanent 'AttendanceRecords' document with the end time
        db.collection("AttendanceRecords")
            .document(activeRecordId)
            .update("sessionEnd", Timestamp.now())
            .addOnSuccessListener {

                // 3. DELETE the temporary 'ActiveAttendanceSessions' document
                db.collection("ActiveAttendanceSessions")
                    .document(classId)
                    .delete()
                    .addOnSuccessListener {
                        // 4. UI Update on final success
                        binding.cardQrCodeSession.visibility = View.GONE // Hide QR card
                        binding.fabStartAttendance.visibility = View.VISIBLE // Show start button

                        // Clear the stored record ID
                        currentSessionRecordId = null

                        // Stop the timer if it's running
                        // countdownTimer.cancel()

                        Toast.makeText(this, "Attendance session ended.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error ending session: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error updating record end time: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

    }


}

