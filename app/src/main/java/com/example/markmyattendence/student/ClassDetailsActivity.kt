package com.example.markmyattendence.student

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.markmyattendence.R

class ClassDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... setup layout ...

        // 1. Receive the Intent data
        val classId = intent.getStringExtra("CLASS_ID")
        val className = intent.getStringExtra("CLASS_NAME")
        val classroom = intent.getStringExtra("CLASS_ROOM")
        val startTime = intent.getStringExtra("START_TIME")
        val endTime = intent.getStringExtra("END_TIME")
        val classCode = intent.getStringExtra("CLASS_CODE")
        val autoApprove = intent.getBooleanExtra("AUTO_APPROVE", false) // Default to false
        val maxStudents = intent.getStringExtra("MAX_STUDENTS") // Assuming it's a String in ClassModel
        val startDate = intent.getStringExtra("START_DATE")
        val repeatDays = intent.getStringArrayListExtra("REPEAT_DAYS") // Get ArrayList<String>

        // 2. Display the Data (using View Binding as an example)
        supportActionBar?.title = className ?: "My class information"

    }
}