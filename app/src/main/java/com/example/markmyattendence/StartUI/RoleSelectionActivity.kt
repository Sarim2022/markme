package com.example.markmyattendence.StartUI

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.databinding.ActivityRoleSelectionBinding
import com.example.markmyattendence.student.StudentHomeActivity
import com.example.markmyattendence.teacher.TeacherHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure user is still logged in from Google
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.btnTeacher.setOnClickListener {
            saveInitialProfile(user, "teacher")
        }

        binding.btnStudent.setOnClickListener {
            saveInitialProfile(user, "student")
        }
    }

    private fun saveInitialProfile(user: FirebaseUser, role: String) {
        val uid = user.uid
        val name = user.displayName ?: ""
        val email = user.email ?: ""

        // Create the base user map with role and Google data.
        // Other fields (phone, collegeId, etc.) are initially set to empty or null.
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "role" to role,
            "name" to name,
            "email" to email,

            // Set required but missing fields to a default/empty state
            "phone" to "",
            "collegeName" to "",

            // Teacher-specific fields
            "department" to "",
            "teacherId" to "",

            // Student-specific fields
            "course" to "",
            "studentId" to ""
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Welcome! Profile setup started.", Toast.LENGTH_SHORT).show()

                // Redirect to home screen based on role
                if (role == "student") {
                    startActivity(Intent(this, StudentHomeActivity::class.java))
                } else {
                    startActivity(Intent(this, TeacherHomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                // IMPORTANT: If save fails, delete the Auth user to prevent orphaned accounts.
                user.delete()
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
    }
}