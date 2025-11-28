package com.example.markmyattendence.StartUI

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.markmyattendence.R
import com.example.markmyattendence.databinding.ActivityLoginBinding
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.widget.Toast
import com.example.markmyattendence.data.AppCache
import com.example.markmyattendence.data.StudentData
import com.example.markmyattendence.student.StudentHomeActivity
import com.example.markmyattendence.teacher.TeacherHomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider



class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityLoginBinding
    companion object {
        private const val TAG = "LoginActivityTag"
    }
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)

            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initUI()
    }

    private fun initUI(){

        binding.tvSignup.setOnClickListener {
            showChooseRoleDialog()
        }
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvForgot.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if(email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            }else{
                performLogin(email,password)
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    handleLoginSuccess()
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun handleLoginSuccess() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userUid = document.id
                    val role = document.getString("role")
                    val user = auth.currentUser ?: return@addOnSuccessListener
                    val uid = user.uid

                    // Set isActive = true for logged in user
                    db.collection("users").document(uid)
                        .update("isActive", true)
                        .addOnSuccessListener {
                            Log.d(TAG, "isActive set to true for user: $uid")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to set isActive: ${e.message}")
                        }

                    if (role == "student") {
                        val intent = Intent(this, StudentHomeActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else if (role == "teacher") {
                        startActivity(Intent(this, TeacherHomeActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(this, "Profile error. Please sign up again.", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this, "Please select your role.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                }
            }
            .addOnFailureListener {
                // Handle Firestore fetch failure
                auth.signOut()
                Toast.makeText(this, "Login failed: Could not retrieve user data.", Toast.LENGTH_LONG).show()
            }
    }


    private fun performLogin(email:String,password:String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && !user.isEmailVerified) {
                        Toast.makeText(this, "Login failed: Please verify your email address. A link has been sent to your inbox.", Toast.LENGTH_LONG).show()

                        user.sendEmailVerification()
                        auth.signOut()

                        return@addOnCompleteListener
                    }


                    val uid = user?.uid
                    if (uid != null) {


                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->


                                val role = document.getString("role")
                                if (role == "student") {

                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            if (doc.exists()) {

                                                val student = StudentData(
                                                    collegeName = doc.getString("collegeName")
                                                        ?: "",
                                                    department = doc.getString("department") ?: "",
                                                    email = doc.getString("email") ?: "",
                                                    name = doc.getString("name") ?: "",
                                                    role = "student",
                                                    studentId = doc.getString("studentId") ?: "",
                                                    uid = uid
                                                )

                                                AppCache.setStudentProfile(student)

                                                val intent = Intent(this, StudentHomeActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                        }
                                } else if (role == "teacher") {
                                    // Set isActive = true for teacher login
                                    db.collection("users").document(uid)
                                        .update("isActive", true)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "isActive set to true for teacher: $uid")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Failed to set isActive for teacher: ${e.message}")
                                        }

                                    val intent = Intent(this,TeacherHomeActivity::class.java)
                                    startActivity(intent)
                                    finish()

                                } else {
                                    // If role is missing, log the user out (safety net)
                                    auth.signOut()
                                    Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener {
                                // Handle Firestore fetch failure
                                auth.signOut()
                                Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        // Should be unreachable if task.isSuccessful
                        Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showChooseRoleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_choose_role, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<TextView>(R.id.btnTeacher).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this,SignupActivity::class.java)
            intent.putExtra("role","teacher")
            startActivity(intent)
            finish()

        }

        dialogView.findViewById<TextView>(R.id.btnStudent).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this,SignupActivity::class.java)
            intent.putExtra("role","student")
            startActivity(intent)
            finish()

        }

        dialogView.findViewById<ImageView>(R.id.cross).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}