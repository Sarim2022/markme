package com.example.markmyattendence.teacher

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityChatUiBinding
import com.example.markmyattendence.databinding.ActivityClassDetailsBinding

class ClassDetailsActivity : AppCompatActivity() {

    private val TAG = "ClassDetailsActivity"

    private lateinit var binding: ActivityClassDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // --- RETRIEVE DATA ---
        val classId = intent.getStringExtra("CLASS_ID")
        val className = intent.getStringExtra("CLASS_NAME")

        binding.backbutton.setOnClickListener {
            finish()
        }
        if (classId != null) {
            Log.d(TAG, "Loaded Class ID: $classId")
            // 💡 TODO: Use this classId to fetch the full class details
            // from Firestore/database and populate the UI in R.layout.activity_class_details

            // Example: updateTitle(className)
            // Example: loadClassData(classId)
        } else {
            // Handle error case where ID is missing
            Log.e(TAG, "Error: Class ID not found in Intent.")
            // Optional: Show a Toast or finish the activity
        }
    }

    // ... (rest of the activity methods)
}