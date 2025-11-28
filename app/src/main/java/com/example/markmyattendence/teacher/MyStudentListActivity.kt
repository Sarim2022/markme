package com.example.markmyattendence.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.data.StudentDisplayModel
import com.example.markmyattendence.databinding.ActivityMyStudentListBinding
import com.example.markmyattendence.teacher.Adatper.StudentListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MyStudentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyStudentListBinding
    private lateinit var adapter: StudentListAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val TAG = "MyStudentListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadStudents()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Students"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        adapter = StudentListAdapter()
        binding.rvStudents.layoutManager = LinearLayoutManager(this)
        binding.rvStudents.adapter = adapter
    }

    private fun loadStudents() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val teacherUid = currentUser.uid

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.rvStudents.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, get the teacher's myStudent array
                val teacherDoc = db.collection("users").document(teacherUid).get().await()
                val myStudents = teacherDoc.get("myStudent") as? List<String> ?: emptyList()

                Log.d(TAG, "Found ${myStudents.size} students for teacher: $teacherUid")

                if (myStudents.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showEmptyState()
                    }
                    return@launch
                }

                // Fetch student details for each student UID
                val studentList = mutableListOf<StudentDisplayModel>()
                for (studentUid in myStudents) {
                    try {
                        val studentDoc = db.collection("users").document(studentUid).get().await()

                        // Check if this is a student account
                        val role = studentDoc.getString("role")
                        if (role == "student") {
                            val name = studentDoc.getString("name") ?: "Unknown"
                            val email = studentDoc.getString("email") ?: "No email"
                            val joinedClasses = studentDoc.get("joinedClasses") as? List<String> ?: emptyList()

                            // Fetch class names for joined classes
                            val classNames = mutableListOf<String>()
                            for (classId in joinedClasses) {
                                try {
                                    val classDoc = db.collection("classes").document(classId).get().await()
                                    val className = classDoc.getString("className") ?: "Unknown Class"
                                    classNames.add(className)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error fetching class $classId: ${e.message}")
                                    classNames.add("Unknown Class")
                                }
                            }

                            val studentDisplay = StudentDisplayModel(
                                uid = studentUid,
                                name = name,
                                email = email,
                                joinedClasses = joinedClasses,
                                classNames = classNames
                            )
                            studentList.add(studentDisplay)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching student $studentUid: ${e.message}")
                    }
                }

                Log.d(TAG, "Successfully loaded ${studentList.size} students")

                withContext(Dispatchers.Main) {
                    if (studentList.isNotEmpty()) {
                        adapter.updateStudents(studentList)
                        showStudents()
                    } else {
                        showEmptyState()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading students: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyStudentListActivity, "Error loading students", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun showStudents() {
        binding.progressBar.visibility = View.GONE
        binding.rvStudents.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.rvStudents.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }
}
