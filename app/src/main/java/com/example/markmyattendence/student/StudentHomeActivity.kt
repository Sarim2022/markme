package com.example.markmyattendence.student

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.example.markmyattendence.R
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.data.AppCache
import com.example.markmyattendence.data.StudentData
import com.example.markmyattendence.databinding.ActivityStudentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentHomeBinding
    private val TAG = "StudentHomeActivity"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(homeFragmentNavStudent.newInstance())

        binding.bottomNavbar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.student_home -> {
                    loadFragment(homeFragmentNavStudent.newInstance())
                    true
                }
                R.id.student_attendce -> {

                    loadFragment(StudentAttendence.newInstance())
                    true
                }
                R.id.student_shedue -> {

                    loadFragment(StudentScheduleF.newInstance()) // Create this fragment
                    true
                }
                R.id.student_account -> {

                    loadFragment(StudentProfile.newInstance()) // Create this fragment
                    true
                }
                else -> false
            }
        }

//        if (AppCache.studentProfile != null) {
////            loadStudentDataToUI(AppCache.studentProfile!!)
//        } else {
//            val receivedUid = auth.currentUser?.uid
//                ?: intent.getStringExtra("user_auth_id")
//
//            if (receivedUid != null) {
////                retrieveAndStoreStudentData(receivedUid)
//            } else {
//                Log.d(TAG, "Unknown logins or user not authenticated.")
//
//            }
//        }

    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            // Replace the content of the FrameLayout with the new Fragment
            .replace(R.id.FragmentLoad, fragment)
            // Optional: Use a transition animation if desired
            // .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }
//    private fun retrieveAndStoreStudentData(uid: String) {
//        db.collection("users").document(uid).get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val studentProfileData = StudentData(
//                        collegeName = document.getString("collegeName") ?: "",
//                        department = document.getString("department") ?: "",
//                        email = document.getString("email") ?: "",
//                        name = document.getString("name") ?: "",
//                        role = document.getString("role") ?: "",
//                        studentId = document.getString("studentId") ?: "",
//                        uid = document.getString("uid") ?: ""
//                    )
//
//                    // Store data in local AppCache
//                    AppCache.setStudentProfile(studentProfileData)
//
//                    // Load data into UI
//                    loadStudentDataToUI(studentProfileData)
//
//                    Toast.makeText(this, "Student data loaded successfully!", Toast.LENGTH_SHORT).show()
//
//                } else {
//                    Toast.makeText(this, "Error: Student profile not found in database.", Toast.LENGTH_LONG).show()
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Error fetching student data: ${e.message}")
//                Toast.makeText(this, "Data retrieval failed. Please check connection.", Toast.LENGTH_LONG).show()
//            }
//    }

//    private fun loadStudentDataToUI(data: StudentData) {
//        binding.tvUserGreet.text = "Hello, ${data.name}"
//        binding.tvUserCollage.text = data.collegeName
//        binding.tvUserSubjectName.text = data.email // Assuming tvUserSubjectName is for email
//        binding.tvUserDepartment.text = data.department
//        binding.tvUserSubjectCode.text = data.studentId // Assuming tvUserSubjectCode is for studentId
//    }
}