package com.example.markmyattendence.StartUI

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.markmyattendence.R
import com.example.markmyattendence.data.OnSignupDataSubmit
import com.example.markmyattendence.databinding.ActivitySignupBinding
import kotlin.collections.emptyList
import com.google.protobuf.LazyStringArrayList.emptyList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity(), OnSignupDataSubmit {

    lateinit var binding: ActivitySignupBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = intent.getStringExtra("role")

        initUi()
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        binding.tvLoginPrompt.setOnClickListener {
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.tvSignupTitle.text = "Create ${role.toString()} account"

        when (role) {
            "teacher" -> loadFragment(TeacherSignupFragment())
            "student" -> loadFragment(StudentSignupFragment())
            else -> Toast.makeText(this, "Invalid role selected!", Toast.LENGTH_SHORT).show()
        }

        binding.ivCross.setOnClickListener {
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragmentContainer, fragment)
            .commit()
    }

    override fun onTeacherSignupSubmit(
        name: String,
        email: String,
        phone: String,
        department: String,
        subjectName: String,
        subjectCode: String,
        collegeId: String,
        collegeName: String,
        teacherId: String,
        password: String
    ) {
        registerUser(email, password) { uid ->
            val teacherMap = mapOf(
                "uid" to uid,
                "role" to "teacher",
                "name" to name,
                "email" to email,
                "department" to department,
                "subject_Name" to subjectName,
                "subject_code" to subjectCode,
                "phone" to phone,
                "collegeId" to collegeId,
                "collegeName" to collegeName,
                "teacherId" to teacherId,
                "myClasses" to listOf<String>(),
                "myStudent" to listOf<String>()
            )

            // Data save only happens if email verification link was successfully sent
            db.collection("users").document(uid).set(teacherMap)
                .addOnSuccessListener {
                    // This toast confirms DB write. Navigation is handled in registerUser.
                    Toast.makeText(this, "Teacher data saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // IMPORTANT: If DB save fails, you should log or handle the auth user cleanup separately.
                    Toast.makeText(this, "DB Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }



    override fun onStudentSignupSubmit(
        name: String,
        email: String,
        collegeName: String,
        department: String,
        studentId: String,
        password: String
    ) {
        registerUser(email, password) { uid ->
            val studentMap = mapOf(
                "uid" to uid,
                "role" to "student",
                "name" to name,
                "email" to email,
                "collegeName" to collegeName,
                "department" to department,
                "studentId" to studentId,
                "joinedClasses" to emptyList(),
                "requestedClasses" to emptyList()
            )

            // Data save only happens if email verification link was successfully sent
            db.collection("users").document(uid).set(studentMap)
                .addOnSuccessListener {
                    // This toast confirms DB write. Navigation is handled in registerUser.
                    Toast.makeText(this, "Student data saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // IMPORTANT: If DB save fails, you should log or handle the auth user cleanup separately.
                    Toast.makeText(this, "DB Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Refactored to handle email verification and subsequent navigation
    private fun registerUser(email: String, password: String, dataSaver: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                val uid = user?.uid

                if (uid != null && user != null) {
                    // 1. Send Verification Email
                    user.sendEmailVerification()
                        .addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                // 2. If verification email sent successfully, save the user data
                                dataSaver(uid)

                                // 3. Inform user, sign out, and navigate to Login
                                Toast.makeText(this, "check gmail to verify,check spam folder !", Toast.LENGTH_LONG).show()
                                auth.signOut() // Sign out the user
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                // 4. Failed to send verification email. Delete the auth user to clean up.
                                user.delete()
                                Toast.makeText(this, "Signup failed: Could not send verification email. Please use a valid address.", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Error: User creation failed.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Auth Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}