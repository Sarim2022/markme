package com.example.markmyattendence.StartUI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.databinding.ActivitySplashBinding
import com.example.markmyattendence.student.StudentHomeActivity
import com.example.markmyattendence.teacher.TeacherHomeActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

open class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    // 1. Declare Firebase instances here
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({

            val user = auth.currentUser
            if (user != null) {
                val uid = user.uid

                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        // 4. Use document.getString() for robustness and null safety if possible,
                        // or check for document existence first.
                        // Assuming "role" exists and is a String:
                        val roleUser = document.getString("role")

                        if (roleUser == "student") {
                            val intent = Intent(this,StudentHomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else if (roleUser == "teacher") {
                            val intent = Intent(this,TeacherHomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }

                    }
                    .addOnFailureListener {
                        // 5. Handle DB fetch failure (e.g., no internet, or permission error)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            } else {
                // 6. User is NOT logged in, proceed to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }

        }, 1500) // Delay in milliseconds
    }
    // Note: You need to ensure startRedirecting is an extension function or a helper function
    // accessible from this class.
}