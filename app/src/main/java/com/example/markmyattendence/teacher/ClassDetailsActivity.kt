package com.example.markmyattendence.teacher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityClassDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ClassDetailsActivity : AppCompatActivity() {

    private val TAG = "ClassDetailsActivity"
    private lateinit var binding: ActivityClassDetailsBinding

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup Toolbar and Back Navigation
        setSupportActionBar(binding.toolbar)
        // Set the back button action
        binding.backbutton.setOnClickListener {
            finish()
        }

        // 2. Receive the Intent data
        val classId = intent.getStringExtra("CLASS_ID")
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

            // 3.7 Start Attendance FAB/Button
            binding.fabStartAttendance.setOnClickListener {
                // TODO: Implement logic to start the attendance session
            }

            // Setup delete button click listener
            binding.fabDeleteDelete.setOnClickListener{
                Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show()
                showDeleteConfirmationDialog(classId, className)
            }

            loadEnrolledStudents(classId)

        } else {
            // Handle case where required data (ID/Name) is missing
            Log.e(TAG, "Error: CLASS_ID or CLASS_NAME not found in Intent.")
            // You might want to show a message to the user and close the activity
            // finish()
        }
    }

    // Example function signature for loading students
    private fun loadEnrolledStudents(classId: String) {

        Log.d(TAG, "Starting student data load for Class ID: $classId")
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


}