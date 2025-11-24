package com.example.markmyattendence.StartUI

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.ivCrossX.setOnClickListener {
            Toast.makeText(this, "back", Toast.LENGTH_SHORT).show()
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnResetPassword.setOnClickListener {
            resetPassword()
        }
    }
    private fun resetPassword() {
        val email = binding.etEmailReset.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
            return
        }


        binding.progressBar.visibility = View.VISIBLE
        binding.btnResetPassword.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnResetPassword.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent successfully! Check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    // Handle failure (e.g., email not found, network error)
                    val errorMessage = task.exception?.message ?: "Failed to send reset email. Please try again."
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }
}