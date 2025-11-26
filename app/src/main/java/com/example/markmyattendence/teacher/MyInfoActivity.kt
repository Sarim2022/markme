package com.example.markmyattendence.teacher

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.markmyattendence.databinding.ActivityMyInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.markmyattendence.data.TeacherModel
class MyInfoActivity : AppCompatActivity() {

    // Keep viewModels() if this Activity is the root container and you want a fresh profile
    // If you need data from a *previous* Activity/Fragment, you must ensure the ViewModel scope is shared (e.g., Application scope)
    private val viewModel: TeacherViewModel by viewModels() // Keep this for now

    private lateinit var binding: ActivityMyInfoBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "MyInfoActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "My Information"


        viewModel.teacherProfile.observe(this) { teacherData ->
            if (teacherData != null) {
                // Call the existing function to display data from the ViewModel's cache
                displayTeacherInfo(teacherData)
                Log.d(TAG, "UI updated from ViewModel cache.")
            }
        }
        initUI()
        loadTeacherInfo()

    }
    private fun initUI()
    {
        binding.ivBack.setOnClickListener {
            finish()
        }


    }


    private fun loadTeacherInfo() {
        val currentUserUid = auth.currentUser?.uid


        db.collection("users")
            .document(currentUserUid.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val teacher = document.toObject(TeacherModel::class.java)

                    if (teacher != null) {
                        // 🔑 NEW CRUCIAL STEP: Update the ViewModel's cache
                        viewModel.saveTeacherProfile(teacher)

                        // NOTE: When viewModel.saveTeacherProfile(teacher) runs, it updates the LiveData,
                        // which automatically triggers the 'viewModel.teacherProfile.observe' block in onCreate,
                        // calling displayTeacherInfo(teacher) and updating the UI.
                        // You no longer need to call displayTeacherInfo(teacher) directly here.

                        Log.d(TAG, "Profile fetched and saved to ViewModel.")

                    } else {
                        Toast.makeText(this, "Failed to parse teacher data.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Teacher profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching teacher data: ", exception)
                Toast.makeText(this, "Error loading data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayTeacherInfo(teacher: TeacherModel) {


        binding.tvName.text = teacher.name
        binding.tvEmail.text = teacher.email
        binding.tvPhone.text = teacher.phone ?: "N/A"
        binding.tvRole.text = teacher.role ?: "N/A"
        binding.tvCollegeName.text = teacher.collegeName
        binding.tvDepartment.text = teacher.department
        binding.tvTeacherId.text = teacher.teacherId
        binding.tvSubjectName.text = teacher.subject_Name
        binding.tvSubjectCode.text = teacher.subject_code
    }
}