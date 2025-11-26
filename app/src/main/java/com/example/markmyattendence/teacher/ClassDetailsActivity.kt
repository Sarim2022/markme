package com.example.markmyattendence.teacher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityClassDetailsBinding // Ensure this import is correct

class ClassDetailsActivity : AppCompatActivity() {

    private val TAG = "ClassDetailsActivity"
    private lateinit var binding: ActivityClassDetailsBinding

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
        // TODO: This is where you would initialize your RecyclerView,
        // fetch the list of students associated with 'classId' from your
        // database (e.g., Firebase/Firestore), and update the adapter.

        Log.d(TAG, "Starting student data load for Class ID: $classId")
    }

    // Optional: Override for the share button if you implement it
    // Note: You must add the ImageView/MenuItem for sharing in your XML/Toolbar setup
    /*
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.class_details_menu, menu) // assuming you have a menu xml
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                // Call a function to share class details
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    */
}